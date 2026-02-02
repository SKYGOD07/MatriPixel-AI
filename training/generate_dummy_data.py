import os
import numpy as np
from PIL import Image

def generate_dummy_images(base_dir, category, count=50):
    category_dir = os.path.join(base_dir, category)
    os.makedirs(category_dir, exist_ok=True)
    
    for i in range(count):
        # Create a random RGB image (224x224)
        # We add some bias to make them "different" for the model to learn something
        if category == 'anemic':
            # Slightly more pale/whitish noise
            data = np.random.randint(150, 255, (224, 224, 3), dtype=np.uint8)
        else:
            # Slightly more reddish/darker noise
            data = np.random.randint(50, 200, (224, 224, 3), dtype=np.uint8)
            data[:, :, 0] = np.random.randint(180, 255, (224, 224), dtype=np.uint8) # Boost Red
            
        img = Image.fromarray(data)
        img.save(os.path.join(category_dir, f"dummy_{i}.jpg"))

if __name__ == "__main__":
    data_path = "training/data"
    print(f"Generating dummy data in {data_path}...")
    generate_dummy_images(data_path, 'anemic', 40)
    generate_dummy_images(data_path, 'normal', 40)
    print("Done!")
