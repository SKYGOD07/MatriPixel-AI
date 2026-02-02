"""
MatriPixel AI - Anemia Detection Training Script
=================================================
Train a MobileNetV3 Small CNN to classify eye images as 'Anemic' vs 'Non-Anemic'.
Exports to TFLite format with metadata for Android deployment.

Usage:
    python train_anemia_model.py --data_dir ./data --epochs 50 --batch_size 32

Author: MatriPixel AI Team
"""

import os
import argparse
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.applications import MobileNetV3Small
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint, ReduceLROnPlateau
import json

# Constants
IMG_SIZE = (224, 224)
INPUT_SHAPE = (224, 224, 3)
CLASSES = ['normal', 'anemic']  # 0 = Non-Anemic, 1 = Anemic


def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Train Anemia Detection Model')
    parser.add_argument('--data_dir', type=str, default='./data',
                        help='Path to data directory with anemic/ and normal/ subdirs')
    parser.add_argument('--epochs', type=int, default=50,
                        help='Number of training epochs')
    parser.add_argument('--batch_size', type=int, default=32,
                        help='Training batch size')
    parser.add_argument('--learning_rate', type=float, default=0.001,
                        help='Initial learning rate')
    parser.add_argument('--output_dir', type=str, default='./models',
                        help='Directory to save trained models')
    parser.add_argument('--fine_tune_layers', type=int, default=20,
                        help='Number of layers to fine-tune from the base model')
    return parser.parse_args()


def create_data_generators(data_dir: str, batch_size: int, validation_split: float = 0.2):
    """
    Create training and validation data generators with augmentation.
    
    Args:
        data_dir: Path to the data directory
        batch_size: Batch size for training
        validation_split: Fraction of data to use for validation
    
    Returns:
        Tuple of (train_generator, validation_generator)
    """
    # Training data augmentation
    train_datagen = ImageDataGenerator(
        rescale=1./255,
        rotation_range=20,
        width_shift_range=0.2,
        height_shift_range=0.2,
        shear_range=0.15,
        zoom_range=0.2,
        horizontal_flip=True,
        brightness_range=[0.8, 1.2],
        fill_mode='nearest',
        validation_split=validation_split
    )
    
    # Validation data - only rescaling
    val_datagen = ImageDataGenerator(
        rescale=1./255,
        validation_split=validation_split
    )
    
    # Create generators
    train_generator = train_datagen.flow_from_directory(
        data_dir,
        target_size=IMG_SIZE,
        batch_size=batch_size,
        class_mode='binary',
        classes=CLASSES,
        subset='training',
        shuffle=True
    )
    
    validation_generator = val_datagen.flow_from_directory(
        data_dir,
        target_size=IMG_SIZE,
        batch_size=batch_size,
        class_mode='binary',
        classes=CLASSES,
        subset='validation',
        shuffle=False
    )
    
    print(f"\n📊 Dataset Summary:")
    print(f"   Training samples: {train_generator.samples}")
    print(f"   Validation samples: {validation_generator.samples}")
    print(f"   Classes: {train_generator.class_indices}")
    
    return train_generator, validation_generator


def create_model(fine_tune_layers: int = 20) -> keras.Model:
    """
    Create a MobileNetV3 Small model with custom classification head.
    
    Args:
        fine_tune_layers: Number of layers to unfreeze for fine-tuning
    
    Returns:
        Compiled Keras model
    """
    # Load pre-trained MobileNetV3 Small
    base_model = MobileNetV3Small(
        input_shape=INPUT_SHAPE,
        include_top=False,
        weights='imagenet',
        pooling='avg'
    )
    
    # Freeze base model initially
    base_model.trainable = False
    
    # Build classification head
    inputs = keras.Input(shape=INPUT_SHAPE, name='input_image')
    
    # Preprocessing layer for MobileNetV3
    x = tf.keras.applications.mobilenet_v3.preprocess_input(inputs)
    
    # Base model
    x = base_model(x, training=False)
    
    # Classification head
    x = layers.Dropout(0.3)(x)
    x = layers.Dense(128, activation='relu', kernel_regularizer=keras.regularizers.l2(0.01))(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.4)(x)
    x = layers.Dense(64, activation='relu', kernel_regularizer=keras.regularizers.l2(0.01))(x)
    x = layers.Dropout(0.3)(x)
    
    # Output layer - sigmoid for binary classification
    outputs = layers.Dense(1, activation='sigmoid', name='anemia_probability')(x)
    
    model = keras.Model(inputs, outputs, name='anemia_detector')
    
    print(f"\n🏗️ Model Architecture:")
    print(f"   Base model: MobileNetV3 Small")
    print(f"   Total params: {model.count_params():,}")
    
    return model, base_model


def compile_model(model: keras.Model, learning_rate: float):
    """Compile the model with optimizer and metrics."""
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=learning_rate),
        loss='binary_crossentropy',
        metrics=[
            'accuracy',
            keras.metrics.Precision(name='precision'),
            keras.metrics.Recall(name='recall'),
            keras.metrics.AUC(name='auc')
        ]
    )
    return model


def get_callbacks(output_dir: str):
    """Create training callbacks."""
    os.makedirs(output_dir, exist_ok=True)
    
    return [
        EarlyStopping(
            monitor='val_auc',
            patience=10,
            restore_best_weights=True,
            mode='max',
            verbose=1
        ),
        ModelCheckpoint(
            filepath=os.path.join(output_dir, 'anemia_model_best.keras'),
            monitor='val_auc',
            save_best_only=True,
            mode='max',
            verbose=1
        ),
        ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=1e-7,
            verbose=1
        )
    ]


def fine_tune_model(model: keras.Model, base_model: keras.Model, 
                    fine_tune_layers: int, learning_rate: float):
    """Unfreeze top layers of base model for fine-tuning."""
    base_model.trainable = True
    
    # Freeze all except the last `fine_tune_layers` layers
    for layer in base_model.layers[:-fine_tune_layers]:
        layer.trainable = False
    
    # Recompile with lower learning rate
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=learning_rate / 10),
        loss='binary_crossentropy',
        metrics=[
            'accuracy',
            keras.metrics.Precision(name='precision'),
            keras.metrics.Recall(name='recall'),
            keras.metrics.AUC(name='auc')
        ]
    )
    
    trainable_count = sum([1 for layer in base_model.layers if layer.trainable])
    print(f"\n🔓 Fine-tuning enabled:")
    print(f"   Trainable layers in base: {trainable_count}")
    
    return model


def convert_to_tflite(model: keras.Model, output_dir: str):
    """
    Convert the trained model to TFLite format with metadata.
    
    Args:
        model: Trained Keras model
        output_dir: Directory to save the TFLite model
    """
    os.makedirs(output_dir, exist_ok=True)
    
    # Convert to TFLite with optimizations
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Enable optimizations
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Optional: Quantize to float16 for smaller model size
    converter.target_spec.supported_types = [tf.float16]
    
    # Convert
    tflite_model = converter.convert()
    
    # Save the model
    tflite_path = os.path.join(output_dir, 'anemia_detector.tflite')
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"\n✅ TFLite model saved: {tflite_path}")
    print(f"   Model size: {os.path.getsize(tflite_path) / 1024 / 1024:.2f} MB")
    
    # Create metadata JSON (labels file)
    labels_path = os.path.join(output_dir, 'labels.txt')
    with open(labels_path, 'w') as f:
        f.write('Non-Anemic\nAnemic')
    
    # Create model metadata JSON
    metadata = {
        'name': 'Anemia Detector',
        'description': 'Detects anemia from eye conjunctiva images',
        'version': '1.0.0',
        'author': 'MatriPixel AI',
        'input': {
            'name': 'input_image',
            'shape': [1, 224, 224, 3],
            'type': 'float32',
            'normalization': {
                'mean': [127.5, 127.5, 127.5],
                'std': [127.5, 127.5, 127.5]
            }
        },
        'output': {
            'name': 'anemia_probability',
            'shape': [1, 1],
            'type': 'float32',
            'labels': ['Non-Anemic', 'Anemic'],
            'threshold': 0.5
        }
    }
    
    metadata_path = os.path.join(output_dir, 'model_metadata.json')
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f, indent=2)
    
    print(f"   Labels saved: {labels_path}")
    print(f"   Metadata saved: {metadata_path}")
    
    return tflite_path


def add_tflite_metadata(tflite_path: str, output_dir: str):
    """
    Add proper TFLite metadata using the tflite-support library.
    This enables automatic code generation in Android Studio.
    
    Note: Requires tflite-support package:
        pip install tflite-support
    """
    try:
        from tflite_support import metadata as _metadata
        from tflite_support import metadata_schema_py_generated as _metadata_fb
        from tflite_support import flatbuffers
        
        # Create model metadata
        model_meta = _metadata_fb.ModelMetadataT()
        model_meta.name = "Anemia Detector"
        model_meta.description = (
            "Detects anemia from eye conjunctiva images. "
            "Output is a probability score where 0 = Non-Anemic and 1 = Anemic."
        )
        model_meta.version = "1.0.0"
        model_meta.author = "MatriPixel AI"
        model_meta.license = "Apache License 2.0"
        
        # Create input tensor metadata
        input_meta = _metadata_fb.TensorMetadataT()
        input_meta.name = "image"
        input_meta.description = "Input eye image to be classified"
        input_meta.content = _metadata_fb.ContentT()
        input_meta.content.contentProperties = _metadata_fb.ImagePropertiesT()
        input_meta.content.contentProperties.colorSpace = _metadata_fb.ColorSpaceType.RGB
        input_meta.content.contentPropertiesType = _metadata_fb.ContentProperties.ImageProperties
        
        # Normalization parameters
        input_normalization = _metadata_fb.ProcessUnitT()
        input_normalization.optionsType = _metadata_fb.ProcessUnitOptions.NormalizationOptions
        input_normalization.options = _metadata_fb.NormalizationOptionsT()
        input_normalization.options.mean = [127.5, 127.5, 127.5]
        input_normalization.options.std = [127.5, 127.5, 127.5]
        input_meta.processUnits = [input_normalization]
        
        # Create output tensor metadata
        output_meta = _metadata_fb.TensorMetadataT()
        output_meta.name = "probability"
        output_meta.description = "Probability of anemia (0 = Non-Anemic, 1 = Anemic)"
        output_meta.content = _metadata_fb.ContentT()
        output_meta.content.contentPropertiesType = _metadata_fb.ContentProperties.FeatureProperties
        
        # Create subgraph metadata
        subgraph = _metadata_fb.SubGraphMetadataT()
        subgraph.inputTensorMetadata = [input_meta]
        subgraph.outputTensorMetadata = [output_meta]
        model_meta.subgraphMetadata = [subgraph]
        
        # Build flatbuffer
        b = flatbuffers.Builder(0)
        b.Finish(model_meta.Pack(b), _metadata.MetadataPopulator.METADATA_FILE_IDENTIFIER)
        metadata_buf = bytes(b.Output())
        
        # Write metadata to model
        output_path = os.path.join(output_dir, 'anemia_detector_with_metadata.tflite')
        populator = _metadata.MetadataPopulator.with_model_file(tflite_path)
        populator.load_metadata_buffer(metadata_buf)
        
        # Associate labels file
        labels_path = os.path.join(output_dir, 'labels.txt')
        populator.load_associated_files([labels_path])
        populator.populate()
        
        # Copy to output path
        import shutil
        shutil.copy(tflite_path, output_path)
        populator = _metadata.MetadataPopulator.with_model_file(output_path)
        populator.load_metadata_buffer(metadata_buf)
        populator.load_associated_files([labels_path])
        populator.populate()
        
        print(f"\n✅ TFLite model with metadata saved: {output_path}")
        return output_path
        
    except ImportError:
        print("\n⚠️  tflite-support not installed. Skipping embedded metadata.")
        print("   Install with: pip install tflite-support")
        print("   Model metadata saved as separate JSON file instead.")
        return tflite_path


def evaluate_model(model: keras.Model, validation_generator):
    """Evaluate the model and print metrics."""
    print("\n📈 Evaluating model...")
    results = model.evaluate(validation_generator, verbose=1)
    
    metrics = dict(zip(model.metrics_names, results))
    
    print("\n📊 Final Metrics:")
    print(f"   Loss:      {metrics['loss']:.4f}")
    print(f"   Accuracy:  {metrics['accuracy']:.4f}")
    print(f"   Precision: {metrics['precision']:.4f}")
    print(f"   Recall:    {metrics['recall']:.4f}")
    print(f"   AUC:       {metrics['auc']:.4f}")
    
    # Calculate F1 score
    if metrics['precision'] + metrics['recall'] > 0:
        f1 = 2 * (metrics['precision'] * metrics['recall']) / (metrics['precision'] + metrics['recall'])
        print(f"   F1 Score:  {f1:.4f}")
    
    return metrics


def main():
    """Main training pipeline."""
    args = parse_args()
    
    print("=" * 60)
    print("🧬 MatriPixel AI - Anemia Detection Model Training")
    print("=" * 60)
    print(f"\n📁 Data directory: {args.data_dir}")
    print(f"🔄 Epochs: {args.epochs}")
    print(f"📦 Batch size: {args.batch_size}")
    print(f"📈 Learning rate: {args.learning_rate}")
    print(f"💾 Output directory: {args.output_dir}")
    
    # Verify data directory exists
    if not os.path.exists(args.data_dir):
        print(f"\n❌ Error: Data directory not found: {args.data_dir}")
        print("   Please create the following structure:")
        print("   data/")
        print("   ├── anemic/")
        print("   │   ├── image1.jpg")
        print("   │   └── ...")
        print("   └── normal/")
        print("       ├── image1.jpg")
        print("       └── ...")
        return
    
    # Check for subdirectories
    anemic_dir = os.path.join(args.data_dir, 'anemic')
    normal_dir = os.path.join(args.data_dir, 'normal')
    
    if not os.path.exists(anemic_dir) or not os.path.exists(normal_dir):
        print(f"\n❌ Error: Expected 'anemic' and 'normal' subdirectories in {args.data_dir}")
        return
    
    # Create data generators
    train_gen, val_gen = create_data_generators(args.data_dir, args.batch_size)
    
    # Create model
    model, base_model = create_model(args.fine_tune_layers)
    model = compile_model(model, args.learning_rate)
    
    model.summary()
    
    # Get callbacks
    callbacks = get_callbacks(args.output_dir)
    
    # Phase 1: Train classification head only
    print("\n" + "=" * 60)
    print("🚀 Phase 1: Training classification head...")
    print("=" * 60)
    
    initial_epochs = min(10, args.epochs // 2)
    
    history = model.fit(
        train_gen,
        epochs=initial_epochs,
        validation_data=val_gen,
        callbacks=callbacks,
        verbose=1
    )
    
    # Phase 2: Fine-tune with unfrozen layers
    if args.epochs > initial_epochs:
        print("\n" + "=" * 60)
        print("🔧 Phase 2: Fine-tuning base model...")
        print("=" * 60)
        
        model = fine_tune_model(model, base_model, args.fine_tune_layers, args.learning_rate)
        
        history_fine = model.fit(
            train_gen,
            epochs=args.epochs,
            initial_epoch=initial_epochs,
            validation_data=val_gen,
            callbacks=callbacks,
            verbose=1
        )
    
    # Evaluate
    metrics = evaluate_model(model, val_gen)
    
    # Save the final Keras model
    keras_path = os.path.join(args.output_dir, 'anemia_model_final.keras')
    model.save(keras_path)
    print(f"\n✅ Keras model saved: {keras_path}")
    
    # Convert to TFLite
    print("\n" + "=" * 60)
    print("📱 Converting to TFLite format...")
    print("=" * 60)
    
    tflite_path = convert_to_tflite(model, args.output_dir)
    
    # Add TFLite metadata (if tflite-support is available)
    add_tflite_metadata(tflite_path, args.output_dir)
    
    print("\n" + "=" * 60)
    print("🎉 Training Complete!")
    print("=" * 60)
    print(f"\n📁 Output files in: {args.output_dir}")
    print("   • anemia_model_final.keras  - Full Keras model")
    print("   • anemia_detector.tflite    - TFLite model for Android")
    print("   • labels.txt                - Class labels")
    print("   • model_metadata.json       - Model metadata")


if __name__ == '__main__':
    main()
