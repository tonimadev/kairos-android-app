import os
import xml.etree.ElementTree as ET

def check_missing_strings(base_dir):
    res_dir = os.path.join(base_dir, 'src', 'main', 'res')
    if not os.path.exists(res_dir):
        return

    # Find the base strings.xml
    base_values_dir = os.path.join(res_dir, 'values')
    base_strings_file = os.path.join(base_values_dir, 'strings.xml')
    
    if not os.path.exists(base_strings_file):
        return
        
    try:
        base_tree = ET.parse(base_strings_file)
        base_root = base_tree.getroot()
        base_strings = {
            child.attrib['name'] 
            for child in base_root 
            if child.tag == 'string' and child.attrib.get('translatable') != 'false'
        }
    except Exception as e:
        print(f"Error parsing {base_strings_file}: {e}")
        return

    if not base_strings:
        return

    # Find all values-* directories
    missing_info = {}
    for item in os.listdir(res_dir):
        if item.startswith('values-') and '-' in item: # values-es, values-pt, etc.
            lang = item.replace('values-', '')
            lang_strings_file = os.path.join(res_dir, item, 'strings.xml')
            
            if not os.path.exists(lang_strings_file):
                missing_info[lang] = base_strings
                continue
                
            try:
                lang_tree = ET.parse(lang_strings_file)
                lang_root = lang_tree.getroot()
                lang_strings = {child.attrib['name'] for child in lang_root if child.tag == 'string'}
            except Exception as e:
                continue

            missing = base_strings - lang_strings
            if missing:
                missing_info[lang] = missing
                
    if missing_info:
        print(f"--- Module: {base_dir} ---")
        for lang, missing in missing_info.items():
            print(f"Language '{lang}' is missing {len(missing)} strings:")
            for m in sorted(missing):
                print(f"  - {m}")
        print("")

if __name__ == "__main__":
    project_root = "/Users/anthoni/AndroidStudioProjects/kairos-android-app"
    for module in ["app", "core", "wear"]:
        check_missing_strings(os.path.join(project_root, module))
