"""
Simple script to generate wprime-icon.png from the battery icon design.
This creates a simplified version of the ic_wprime.xml vector drawable.
"""
from PIL import Image, ImageDraw

# Create a 128x200 image with transparent background
width, height = 128, 200
img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Battery body (white with dark border)
# Rounded rectangle: M32,22 h64 a18,18 0 0 1 18,18 v140 a18,18 0 0 1 -18,18 h-64 a18,18 0 0 1 -18,-18 v-140 a18,18 0 0 1 18,-18z
draw.rounded_rectangle([(14, 22), (114, 198)], radius=18, fill='white', outline='#222222', width=10)

# Battery top connector
# M52,7 h24 a6,6 0 0 1 6,6 v8 a6,6 0 0 1 -6,6 h-24 a6,6 0 0 1 -6,-6 v-8 a6,6 0 0 1 6,-6z
draw.rounded_rectangle([(46, 7), (82, 27)], radius=6, fill='#222222')

# Battery segments (from top to bottom)
segments = [
    # Green (full)
    {'y': 38, 'color': '#57CA3B', 'light': '#7DDF77'},
    # Yellow-green
    {'y': 68, 'color': '#D6E84B', 'light': '#E5EF87'},
    # Yellow
    {'y': 98, 'color': '#F9C233', 'light': '#FFE170'},
    # Orange
    {'y': 128, 'color': '#FC8923', 'light': '#FFBC7C'},
    # Red (critical)
    {'y': 158, 'color': '#EF2A2A', 'light': '#FF6161'},
]

for seg in segments:
    y = seg['y']
    # Full width segment
    draw.rounded_rectangle([(30, y), (98, y + 24)], radius=6, fill=seg['color'])
    # Left half with lighter color
    draw.rounded_rectangle([(30, y), (64, y + 24)], radius=6, fill=seg['light'])

# Save the image
img.save('assets/wprime-icon.png', 'PNG')
print("✅ Icon created successfully at assets/wprime-icon.png")

