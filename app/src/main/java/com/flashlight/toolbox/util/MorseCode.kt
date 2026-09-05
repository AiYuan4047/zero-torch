package com.flashlight.toolbox.util

/** 国际摩斯码映射表（ITU-R M.1677-1） */
object MorseCode {

    private val MORSE_MAP = mapOf(
        'A' to ".-",    'B' to "-...",  'C' to "-.-.", 'D' to "-..",
        'E' to ".",     'F' to "..-.",  'G' to "--.",  'H' to "....",
        'I' to "..",    'J' to ".---",  'K' to "-.-",  'L' to ".-..",
        'M' to "--",    'N' to "-.",    'O' to "---",  'P' to ".--.",
        'Q' to "--.-",  'R' to ".-.",   'S' to "...",  'T' to "-",
        'U' to "..-",   'V' to "...-",  'W' to ".--",  'X' to "-..-",
        'Y' to "-.--",  'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '!' to "-.-.--", '/' to "-..-.",  '(' to "-.--.",  ')' to "-.--.-",
        '&' to ".-...",  ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.",  '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
        '$' to "...-..-", '@' to ".--.-.", ' ' to "/"
    )

    /** 将文本转为摩斯码序列（点/划/分隔符） */
    fun encode(text: String): List<MorseSymbol> {
        val result = mutableListOf<MorseSymbol>()
        val words = text.trim().split(Regex("\\s+"))
        
        words.forEachIndexed { wordIndex, word ->
            word.uppercase().forEachIndexed { charIndex, char ->
                MORSE_MAP[char]?.let { code ->
                    code.forEachIndexed { symbolIndex, symbol ->
                        when (symbol) {
                            '.' -> result.add(MorseSymbol.Dot)
                            '-' -> result.add(MorseSymbol.Dash)
                        }
                        // 符号间间隔（单位时间）
                        if (symbolIndex != code.lastIndex) {
                            result.add(MorseSymbol.Gap)
                        }
                    }
                }
                // 字符间间隔（3单位时间）
                if (charIndex != word.lastIndex) {
                    result.add(MorseSymbol.CharGap)
                }
            }
            // 单词间间隔（7单位时间）
            if (wordIndex != words.lastIndex) {
                result.add(MorseSymbol.WordGap)
            }
        }
        return result
    }

    /** 预设摩斯码消息 */
    data class PresetMessage(
        val name: String,
        val text: String,
        val description: String = ""
    ) {
        val symbols: List<MorseSymbol> = encode(text)
    }

    /** 内置预设 */
    val BUILTIN_PRESETS = listOf(
        PresetMessage("SOS", "SOS", "国际通用求救信号"),
        PresetMessage("HI", "HI", "打招呼/测试信号，业余无线常用"),
        PresetMessage("HELLO", "HELLO", "你好"),
        PresetMessage("HELP", "HELP", "求助"),
        PresetMessage("OK", "OK", "确认/收到"),
    )
}

sealed class MorseSymbol {
    object Dot : MorseSymbol()      // 点 · (1单位)
    object Dash : MorseSymbol()     // 划 — (3单位)
    object Gap : MorseSymbol()      // 符号间隔 (1单位)
    object CharGap : MorseSymbol()  // 字符间隔 (3单位)
    object WordGap : MorseSymbol()  // 单词间隔 (7单位)
}