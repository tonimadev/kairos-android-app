import os
import sys
import xml.etree.ElementTree as ET

LANGUAGES = ['ar', 'de', 'en', 'es', 'fr', 'hi', 'ja', 'ru', 'zh']


def check_missing_strings(base_dir):
    """Prints missing translations for a module and returns True if any were found."""
    res_dir = os.path.join(base_dir, 'src', 'main', 'res')
    if not os.path.exists(res_dir):
        return False

    # Find the base strings.xml
    base_values_dir = os.path.join(res_dir, 'values')
    base_strings_file = os.path.join(base_values_dir, 'strings.xml')
    
    if not os.path.exists(base_strings_file):
        return False
        
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
        return True

    if not base_strings:
        return False

    # Check every supported translation
    missing_info = {}
    for lang in LANGUAGES:
        lang_strings_file = os.path.join(res_dir, f'values-{lang}', 'strings.xml')
        
        if not os.path.exists(lang_strings_file):
            missing_info[lang] = base_strings
            continue
            
        try:
            lang_tree = ET.parse(lang_strings_file)
            lang_root = lang_tree.getroot()
            lang_strings = {child.attrib['name'] for child in lang_root if child.tag == 'string'}
        except Exception as e:
            print(f"Error parsing {lang_strings_file}: {e}")
            missing_info[lang] = base_strings
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
    return bool(missing_info)

if __name__ == "__main__":
    project_root = os.path.dirname(os.path.abspath(__file__))
    has_missing = False
    for module in ["app", "core", "wear"]:
        has_missing |= check_missing_strings(os.path.join(project_root, module))
    sys.exit(1 if has_missing else 0)
