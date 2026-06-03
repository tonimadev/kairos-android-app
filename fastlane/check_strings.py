import os
import xml.etree.ElementTree as ET

res_dir = "/Users/anthoni/AndroidStudioProjects/kairos-android-app/core/src/main/res"
default_strings = os.path.join(res_dir, "values", "strings.xml")

if not os.path.exists(default_strings):
    print("Default strings not found!")
    exit(1)

tree = ET.parse(default_strings)
root = tree.getroot()
default_keys = set(child.attrib['name'] for child in root if child.tag == 'string')
print(f"Total default strings: {len(default_keys)}")

langs = ["values-ar", "values-de", "values-en", "values-es", "values-fr", "values-hi", "values-ja", "values-ru", "values-zh"]
for lang in langs:
    lang_strings = os.path.join(res_dir, lang, "strings.xml")
    if os.path.exists(lang_strings):
        try:
            tree_lang = ET.parse(lang_strings)
            root_lang = tree_lang.getroot()
            lang_keys = set(child.attrib['name'] for child in root_lang if child.tag == 'string')
            missing = default_keys - lang_keys
            if missing:
                print(f"Missing in {lang}: {len(missing)}")
                for m in missing:
                    print(f"  - {m}")
            else:
                print(f"All good in {lang}")
        except Exception as e:
            print(f"Error parsing {lang}: {e}")
    else:
        print(f"File not found: {lang_strings}")
