import os
import subprocess
import sys

def install_and_import(package, import_name=None):
    if import_name is None:
        import_name = package
    try:
        __import__(import_name)
        print(f"OK: {package} is already installed.")
    except ImportError:
        print(f"Installing {package}...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", package])
        print(f"OK: {package} successfully installed.")

# Install dependencies
install_and_import('pillow', 'PIL')
install_and_import('svglib')

from PIL import Image
from svglib.svglib import svg2rlg
from reportlab.graphics import renderPM

# Define paths
SVG_PATH = r"C:\Users\Suleman\Downloads\SkyBook_Java_OOP\SkyBook\src\skybook\assets\images\airplane.svg"
PNG_PATH = r"C:\Users\Suleman\Downloads\SkyBook_Java_OOP\SkyBook\src\skybook\assets\images\airplane.png"
ICO_PATH = r"C:\Users\Suleman\Downloads\SkyBook_Java_OOP\SkyBook\src\skybook\assets\images\airplane.ico"

print("Parsing SVG and applying branding colors...")

# Read the SVG
with open(SVG_PATH, 'r', encoding='utf-8') as f:
    svg_content = f.read()

# Replace currentColor with our brand color #38bdf8
# Also increase width and height to 256 for a high-res rendering
modified_svg = svg_content.replace('stroke="currentColor"', 'stroke="#38bdf8"')
modified_svg = modified_svg.replace('width="24"', 'width="256"')
modified_svg = modified_svg.replace('height="24"', 'height="256"')

# Write out a temporary high-res modified SVG
TEMP_SVG = "temp_icon.svg"
with open(TEMP_SVG, 'w', encoding='utf-8') as f:
    f.write(modified_svg)

try:
    # Convert temporary SVG to high-res transparent PNG
    print("Generating PNG...")
    drawing = svg2rlg(TEMP_SVG)
    renderPM.drawToFile(drawing, PNG_PATH, fmt='PNG')
    print(f"OK: Created PNG at {PNG_PATH}")

    # Generate multi-resolution ICO file
    print("Generating ICO...")
    img = Image.open(PNG_PATH)
    
    # Save as multi-resolution ICO
    img.save(ICO_PATH, format='ICO', sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    print(f"OK: Created ICO at {ICO_PATH}")
    
finally:
    # Clean up temporary SVG
    if os.path.exists(TEMP_SVG):
        os.remove(TEMP_SVG)

print("Icon conversion complete!")
