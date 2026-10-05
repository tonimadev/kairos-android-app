package digital.tonima.core.notifications

/**
 * Words the event parser understands in one language. Everything is written the way people type it
 * (accents, native scripts); the parser folds both the vocabulary and the notification text the same way.
 *
 * Weekdays run Monday..Sunday and months January..December; every entry may list several spellings.
 */
internal data class NotificationVocabulary(
    val intent: List<String>,
    val today: List<String>,
    val tomorrow: List<String>,
    val weekdays: List<List<String>>,
    val months: List<List<String>>,
    val afternoon: List<String>,
    val morning: List<String>,
    val fillers: List<String>,
)

internal object NotificationVocabularies {
    val ALL: List<NotificationVocabulary> by lazy {
        listOf(portuguese, english, spanish, french, german, russian, arabic, hindi, japanese, chinese)
    }

    private val portuguese =
        NotificationVocabulary(
            intent =
                listOf(
                    "reunião",
                    "consulta",
                    "compromisso",
                    "evento",
                    "almoço",
                    "jantar",
                    "encontro",
                    "entrevista",
                    "convite",
                    "agendad",
                    "voo",
                    "aula",
                    "vamos",
                    "call",
                    "chamada",
                ),
            today = listOf("hoje"),
            tomorrow = listOf("amanhã"),
            weekdays =
                listOf(
                    listOf("segunda"),
                    listOf("terça"),
                    listOf("quarta"),
                    listOf("quinta"),
                    listOf("sexta"),
                    listOf("sábado"),
                    listOf("domingo"),
                ),
            months =
                listOf(
                    listOf("janeiro"),
                    listOf("fevereiro"),
                    listOf("março"),
                    listOf("abril"),
                    listOf("maio"),
                    listOf("junho"),
                    listOf("julho"),
                    listOf("agosto"),
                    listOf("setembro"),
                    listOf("outubro"),
                    listOf("novembro"),
                    listOf("dezembro"),
                ),
            afternoon = listOf("da tarde", "da noite"),
            morning = listOf("da manhã"),
            fillers = listOf("às", "as", "em", "no", "na", "para", "dia"),
        )

    private val english =
        NotificationVocabulary(
            intent =
                listOf(
                    "meeting",
                    "call",
                    "appointment",
                    "event",
                    "lunch",
                    "dinner",
                    "interview",
                    "invite",
                    "invitation",
                    "scheduled",
                    "flight",
                    "webinar",
                    "class",
                    "let's",
                    "check-in",
                ),
            today = listOf("today", "tonight"),
            tomorrow = listOf("tomorrow"),
            weekdays =
                listOf(
                    listOf("monday"),
                    listOf("tuesday"),
                    listOf("wednesday"),
                    listOf("thursday"),
                    listOf("friday"),
                    listOf("saturday"),
                    listOf("sunday"),
                ),
            months =
                listOf(
                    listOf("january", "jan"),
                    listOf("february", "feb"),
                    listOf("march"),
                    listOf("april", "apr"),
                    listOf("may"),
                    listOf("june", "jun"),
                    listOf("july", "jul"),
                    listOf("august", "aug"),
                    listOf("september", "sept", "sep"),
                    listOf("october", "oct"),
                    listOf("november", "nov"),
                    listOf("december", "dec"),
                ),
            afternoon = listOf("in the afternoon", "in the evening", "at night"),
            morning = listOf("in the morning"),
            fillers = listOf("at", "on", "for", "by", "@"),
        )

    private val spanish =
        NotificationVocabulary(
            intent =
                listOf(
                    "reunión",
                    "cita",
                    "consulta",
                    "evento",
                    "almuerzo",
                    "cena",
                    "comida",
                    "entrevista",
                    "invitación",
                    "agendad",
                    "programad",
                    "vuelo",
                    "clase",
                    "quedamos",
                    "nos vemos",
                ),
            today = listOf("hoy"),
            tomorrow = listOf("mañana"),
            weekdays =
                listOf(
                    listOf("lunes"),
                    listOf("martes"),
                    listOf("miércoles"),
                    listOf("jueves"),
                    listOf("viernes"),
                    listOf("sábado"),
                    listOf("domingo"),
                ),
            months =
                listOf(
                    listOf("enero"),
                    listOf("febrero"),
                    listOf("marzo"),
                    listOf("abril"),
                    listOf("mayo"),
                    listOf("junio"),
                    listOf("julio"),
                    listOf("agosto"),
                    listOf("septiembre", "setiembre"),
                    listOf("octubre"),
                    listOf("noviembre"),
                    listOf("diciembre"),
                ),
            afternoon = listOf("de la tarde", "de la noche"),
            morning = listOf("de la mañana"),
            fillers = listOf("a", "las", "la", "el", "para", "en"),
        )

    private val french =
        NotificationVocabulary(
            intent =
                listOf(
                    "réunion",
                    "rendez-vous",
                    "rdv",
                    "événement",
                    "déjeuner",
                    "dîner",
                    "entretien",
                    "invitation",
                    "prévu",
                    "planifié",
                    "vol",
                    "cours",
                    "on se voit",
                    "appel",
                ),
            today = listOf("aujourd'hui"),
            tomorrow = listOf("demain"),
            weekdays =
                listOf(
                    listOf("lundi"),
                    listOf("mardi"),
                    listOf("mercredi"),
                    listOf("jeudi"),
                    listOf("vendredi"),
                    listOf("samedi"),
                    listOf("dimanche"),
                ),
            months =
                listOf(
                    listOf("janvier"),
                    listOf("février"),
                    listOf("mars"),
                    listOf("avril"),
                    listOf("mai"),
                    listOf("juin"),
                    listOf("juillet"),
                    listOf("août"),
                    listOf("septembre"),
                    listOf("octobre"),
                    listOf("novembre"),
                    listOf("décembre"),
                ),
            afternoon = listOf("du soir", "de l'après-midi"),
            morning = listOf("du matin"),
            fillers = listOf("à", "a", "le", "pour", "vers"),
        )

    private val german =
        NotificationVocabulary(
            intent =
                listOf(
                    "besprechung",
                    "termin",
                    "treffen",
                    "meeting",
                    "mittagessen",
                    "abendessen",
                    "einladung",
                    "vorstellungsgespräch",
                    "geplant",
                    "flug",
                    "unterricht",
                    "konferenz",
                ),
            today = listOf("heute"),
            tomorrow = listOf("morgen"),
            weekdays =
                listOf(
                    listOf("montag"),
                    listOf("dienstag"),
                    listOf("mittwoch"),
                    listOf("donnerstag"),
                    listOf("freitag"),
                    listOf("samstag", "sonnabend"),
                    listOf("sonntag"),
                ),
            months =
                listOf(
                    listOf("januar"),
                    listOf("februar"),
                    listOf("märz"),
                    listOf("april"),
                    listOf("mai"),
                    listOf("juni"),
                    listOf("juli"),
                    listOf("august"),
                    listOf("september"),
                    listOf("oktober"),
                    listOf("november"),
                    listOf("dezember"),
                ),
            afternoon = listOf("abends", "nachmittags", "nachts"),
            morning = listOf("morgens", "vormittags"),
            fillers = listOf("um", "am", "für", "ab"),
        )

    private val russian =
        NotificationVocabulary(
            intent =
                listOf(
                    "встреч",
                    "совещани",
                    "созвон",
                    "приём",
                    "обед",
                    "ужин",
                    "собеседован",
                    "приглашени",
                    "запланирован",
                    "рейс",
                    "урок",
                    "событи",
                ),
            today = listOf("сегодня"),
            tomorrow = listOf("завтра"),
            weekdays =
                listOf(
                    listOf("понедельник"),
                    listOf("вторник"),
                    listOf("среда", "среду", "среды"),
                    listOf("четверг"),
                    listOf("пятниц"),
                    listOf("суббот"),
                    listOf("воскресень"),
                ),
            months =
                listOf(
                    listOf("января"),
                    listOf("февраля"),
                    listOf("марта"),
                    listOf("апреля"),
                    listOf("мая"),
                    listOf("июня"),
                    listOf("июля"),
                    listOf("августа"),
                    listOf("сентября"),
                    listOf("октября"),
                    listOf("ноября"),
                    listOf("декабря"),
                ),
            afternoon = listOf("вечера", "дня"),
            morning = listOf("утра"),
            fillers = listOf("в", "на", "к", "во"),
        )

    private val arabic =
        NotificationVocabulary(
            intent =
                listOf(
                    "اجتماع",
                    "موعد",
                    "غداء",
                    "عشاء",
                    "مقابلة",
                    "دعوة",
                    "رحلة",
                    "محاضرة",
                    "لقاء",
                    "مكالمة",
                ),
            today = listOf("اليوم"),
            tomorrow = listOf("غدا", "غدًا", "بكرة"),
            weekdays =
                listOf(
                    listOf("الاثنين"),
                    listOf("الثلاثاء"),
                    listOf("الأربعاء"),
                    listOf("الخميس"),
                    listOf("الجمعة"),
                    listOf("السبت"),
                    listOf("الأحد"),
                ),
            months =
                listOf(
                    listOf("يناير"),
                    listOf("فبراير"),
                    listOf("مارس"),
                    listOf("أبريل"),
                    listOf("مايو"),
                    listOf("يونيو"),
                    listOf("يوليو"),
                    listOf("أغسطس"),
                    listOf("سبتمبر"),
                    listOf("أكتوبر"),
                    listOf("نوفمبر"),
                    listOf("ديسمبر"),
                ),
            afternoon = listOf("مساء"),
            morning = listOf("صباح"),
            fillers = listOf("في", "يوم"),
        )

    private val hindi =
        NotificationVocabulary(
            intent =
                listOf(
                    "मीटिंग",
                    "बैठक",
                    "अपॉइंटमेंट",
                    "मुलाकात",
                    "लंच",
                    "डिनर",
                    "इंटरव्यू",
                    "निमंत्रण",
                    "इवेंट",
                    "उड़ान",
                    "कक्षा",
                    "कॉल",
                ),
            today = listOf("आज"),
            tomorrow = emptyList(), // "कल" also means "yesterday", so it is not trusted
            weekdays =
                listOf(
                    listOf("सोमवार"),
                    listOf("मंगलवार"),
                    listOf("बुधवार"),
                    listOf("गुरुवार"),
                    listOf("शुक्रवार"),
                    listOf("शनिवार"),
                    listOf("रविवार"),
                ),
            months =
                listOf(
                    listOf("जनवरी"),
                    listOf("फ़रवरी", "फरवरी"),
                    listOf("मार्च"),
                    listOf("अप्रैल"),
                    listOf("मई"),
                    listOf("जून"),
                    listOf("जुलाई"),
                    listOf("अगस्त"),
                    listOf("सितंबर"),
                    listOf("अक्टूबर"),
                    listOf("नवंबर"),
                    listOf("दिसंबर"),
                ),
            afternoon = listOf("शाम", "दोपहर", "रात"),
            morning = listOf("सुबह"),
            fillers = listOf("को", "पर"),
        )

    private val japanese =
        NotificationVocabulary(
            intent =
                listOf(
                    "会議",
                    "ミーティング",
                    "打ち合わせ",
                    "打合せ",
                    "予約",
                    "約束",
                    "ランチ",
                    "ディナー",
                    "面接",
                    "招待",
                    "イベント",
                    "フライト",
                    "授業",
                ),
            today = listOf("今日"),
            tomorrow = listOf("明日"),
            weekdays =
                listOf(
                    listOf("月曜"),
                    listOf("火曜"),
                    listOf("水曜"),
                    listOf("木曜"),
                    listOf("金曜"),
                    listOf("土曜"),
                    listOf("日曜"),
                ),
            months = emptyList(), // written as 10月20日, handled by a numeric pattern
            afternoon = listOf("午後", "夜"),
            morning = listOf("午前", "朝"),
            fillers = emptyList(),
        )

    private val chinese =
        NotificationVocabulary(
            intent =
                listOf(
                    "会议",
                    "开会",
                    "会面",
                    "预约",
                    "约",
                    "午餐",
                    "晚餐",
                    "面试",
                    "邀请",
                    "活动",
                    "航班",
                    "课程",
                    "电话会议",
                    "聚餐",
                ),
            today = listOf("今天"),
            tomorrow = listOf("明天"),
            weekdays =
                listOf(
                    listOf("星期一", "周一"),
                    listOf("星期二", "周二"),
                    listOf("星期三", "周三"),
                    listOf("星期四", "周四"),
                    listOf("星期五", "周五"),
                    listOf("星期六", "周六"),
                    listOf("星期日", "星期天", "周日", "周天"),
                ),
            months = emptyList(), // written as 10月20日, handled by a numeric pattern
            afternoon = listOf("下午", "晚上", "午后"),
            morning = listOf("上午", "早上", "凌晨"),
            fillers = listOf("在", "于"),
        )
}
