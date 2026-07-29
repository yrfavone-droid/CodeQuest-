import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_brand_icon():
    # Base canvas size for high-res master
    size = 512
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Main Background Container (Rounded Super-ellipse / Squircle)
    margin = 24
    bg_box = [margin, margin, size - margin, size - margin]
    radius = 96
    
    # Outer Glow Layer
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.rounded_rectangle(bg_box, radius=radius, fill=(124, 92, 255, 120))
    glow = glow.filter(ImageFilter.GaussianBlur(18))
    img.paste(glow, (0, 0), glow)

    # Container Body (#0F0F1E dark navy)
    draw.rounded_rectangle(bg_box, radius=radius, fill=(15, 15, 30, 255), outline=(124, 92, 255, 200), width=6)

    # 2. Inner Decorative Math Grid / Matrix Dots
    dot_color = (0, 217, 255, 45)
    for x in range(96, size - 80, 64):
        for y in range(96, size - 80, 64):
            draw.ellipse([x - 3, y - 3, x + 3, y + 3], fill=dot_color)

    # 3. Outer Accent Ring Lines
    draw.arc([margin + 16, margin + 16, size - margin - 16, size - margin - 16], start=30, end=150, fill=(0, 217, 255, 220), width=4)
    draw.arc([margin + 16, margin + 16, size - margin - 16, size - margin - 16], start=210, end=330, fill=(124, 92, 255, 220), width=4)

    # Load system font or default for Sigma (Σ), Brackets ({ }), and cursor
    try:
        font_large = ImageFont.truetype("arial.ttf", 220)
        font_medium = ImageFont.truetype("arial.ttf", 190)
        font_mono = ImageFont.truetype("consola.ttf", 100)
    except:
        font_large = ImageFont.load_default()
        font_medium = ImageFont.load_default()
        font_mono = ImageFont.load_default()

    # Draw Left Curly Brace '{' in Purple (#7C5CFF)
    draw.text((70, 115), "{", fill=(124, 92, 255, 255), font=font_large)

    # Draw Right Curly Brace '}' in Cyan (#00D9FF)
    draw.text((360, 115), "}", fill=(0, 217, 255, 255), font=font_large)

    # Draw Center Sigma 'Σ' in Gold/Amber (#FFD700)
    draw.text((195, 135), "Σ", fill=(255, 215, 0, 255), font=font_medium)

    # Draw Terminal Cursor Line '|' in Lime Green (#00FF41)
    draw.rectangle([244, 345, 268, 385], fill=(0, 255, 65, 255))

    # Small f(x) notation accent at bottom right
    try:
        font_fx = ImageFont.truetype("ariali.ttf", 40)
        draw.text((310, 390), "f(x)", fill=(0, 217, 255, 230), font=font_fx)
    except:
        pass

    return img

def main():
    icon_dir = os.path.join("assets", "icon")
    branding_dir = os.path.join("desktopApp", "src", "jvmMain", "resources", "branding")
    public_dir = "public"

    os.makedirs(icon_dir, exist_ok=True)
    os.makedirs(branding_dir, exist_ok=True)
    os.makedirs(public_dir, exist_ok=True)

    master_img = create_brand_icon()
    master_path = os.path.join(icon_dir, "codequest-512.png")
    master_img.save(master_path, "PNG")

    # Generate PNG sizes
    sizes = [512, 256, 128, 64, 32, 16]
    images = []
    for s in sizes:
        resized = master_img.resize((s, s), Image.Resampling.LANCZOS)
        out_png = os.path.join(icon_dir, f"codequest-{s}.png")
        resized.save(out_png, "PNG")
        images.append(resized)

    # Save ICO for Windows
    ico_path_1 = os.path.join(icon_dir, "codequest.ico")
    ico_path_branding = os.path.join(branding_dir, "codequest-academy-logo.ico")
    public_favicon = os.path.join(public_dir, "favicon.ico")

    # Save ICO file containing multiple sizes
    master_img.save(ico_path_1, format="ICO", sizes=[(256, 256), (128, 128), (64, 64), (32, 32), (16, 16)])
    master_img.save(ico_path_branding, format="ICO", sizes=[(256, 256), (128, 128), (64, 64), (32, 32), (16, 16)])
    master_img.save(public_favicon, format="ICO", sizes=[(32, 32), (16, 16)])

    # Copy PNG branding logo
    png_branding = os.path.join(branding_dir, "codequest-academy-logo.png")
    master_img.save(png_branding, "PNG")

    print("✅ Professional Icon assets generated successfully!")

if __name__ == "__main__":
    main()
