import os

base_dir = "/Users/anthoni/AndroidStudioProjects/kairos-android-app/fastlane/metadata/android"

translations = {
    "es-419": "- Alarmas más confiables: se corrigieron problemas que causaban retrasos o pérdida de alarmas.\n- Restauración automática: sus alarmas se reprograman automáticamente después de reiniciar el dispositivo o actualizar la aplicación.\n- Compatibilidad con Android 14+: se resolvieron fallas silenciosas que impedían el sonido y la vibración en dispositivos más nuevos.\n- Wear OS: ¡aplicamos las mismas mejoras de estabilidad a la aplicación de su reloj inteligente!\n",
    "de-DE": "- Zuverlässigere Alarme: Probleme behoben, durch die Alarme verzögert oder verpasst werden konnten.\n- Automatische Wiederherstellung: Ihre Alarme werden nach einem Geräteneustart oder App-Update automatisch neu geplant.\n- Kompatibilität mit Android 14+: Stille Fehler behoben, die Ton und Vibration auf neueren Geräten verhinderten.\n- Wear OS: Die gleichen Stabilitätsverbesserungen wurden auch in der Smartwatch-App angewendet!\n",
    "fr-FR": "- Alarmes plus fiables : résolution de problèmes pouvant retarder ou annuler des alarmes.\n- Restauration automatique : vos alarmes sont automatiquement reprogrammées après un redémarrage de l'appareil ou une mise à jour de l'application.\n- Compatibilité Android 14+ : résolution de défaillances silencieuses empêchant le son et la vibration sur les appareils récents.\n- Wear OS : application des mêmes améliorations de stabilité à l'application pour montre connectée !\n",
    "ru-RU": "- Более надежные будильники: исправлены проблемы, из-за которых будильники могли задерживаться или не срабатывать.\n- Автоматическое восстановление: будильники теперь автоматически переназначаются после перезагрузки устройства или обновления приложения.\n- Совместимость с Android 14+: устранены скрытые ошибки, препятствовавшие воспроизведению звука и вибрации на новых устройствах.\n- Wear OS: те же улучшения стабильности применены к приложению для умных часов!\n",
    "ja-JP": "- アラームの信頼性向上: アラームが遅れたり鳴らなかったりする問題を修正しました。\n- 自動復元: デバイスの再起動やアプリの更新後にアラームが自動的に再スケジュールされるようになりました。\n- Android 14+ 互換性: 新しいデバイスで音や振動が鳴らない無音のエラーを解決しました。\n- Wear OS: スマートウォッチアプリにも同じ安定性向上を適用しました！\n",
    "zh-CN": "- 更可靠的闹钟：修复了可能导致闹钟延迟或错过的错误。\n- 自动恢复：在设备重启或应用更新后，闹钟现在会自动重新调度。\n- Android 14+ 兼容性：解决了导致新设备上无法发出声音和振动的无声故障。\n- Wear OS：对智能手表应用也应用了同样的稳定性改进！\n",
    "hi-IN": "- अधिक विश्वसनीय अलार्म: अलार्म के देरी से बजने या छूट जाने की समस्याओं को ठीक किया गया।\n- ऑटो-रिस्टोर: डिवाइस के रीस्टार्ट या ऐप अपडेट के बाद आपके अलार्म अपने आप रीशेड्यूल हो जाएंगे।\n- Android 14+ संगतता: नए डिवाइस पर ध्वनि और कंपन को रोकने वाली समस्याओं का समाधान।\n- Wear OS: आपकी स्मार्टवॉच ऐप में भी समान स्थिरता सुधार लागू किए गए हैं!\n",
    "ar": "- منبهات أكثر موثوقية: تم إصلاح المشكلات التي تؤدي إلى تأخير المنبهات أو فقدانها.\n- استعادة تلقائية: تتم إعادة جدولة منبهاتك تلقائيًا بعد إعادة تشغيل الجهاز أو تحديث التطبيق.\n- توافق مع Android 14+: تم حل الأعطال الصامتة التي كانت تمنع الصوت والاهتزاز في الأجهزة الحديثة.\n- Wear OS: تم تطبيق نفس تحسينات الاستقرار على تطبيق ساعتك الذكية!\n"
}

for lang, text in translations.items():
    changelog_path = os.path.join(base_dir, lang, "changelogs", "default.txt")
    if os.path.exists(changelog_path):
        with open(changelog_path, "r", encoding="utf-8") as f:
            old_content = f.read()
        with open(changelog_path, "w", encoding="utf-8") as f:
            f.write(text + old_content)
        print(f"Updated {lang}")
    else:
        print(f"Not found: {changelog_path}")
