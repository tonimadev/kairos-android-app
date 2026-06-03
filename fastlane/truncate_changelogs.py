import os

base_dir = "/Users/anthoni/AndroidStudioProjects/kairos-android-app/fastlane/metadata/android"

def truncate_changelog(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if len(content) <= 500:
        return

    # Truncate to 500
    truncated = content[:500]
    # Find last newline to avoid cutting in the middle of a line
    last_newline = truncated.rfind('\n')
    if last_newline != -1:
        truncated = truncated[:last_newline]
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(truncated)
    print(f"Truncated: {filepath}")

for root, dirs, files in os.walk(base_dir):
    if "default.txt" in files:
        truncate_changelog(os.path.join(root, "default.txt"))
