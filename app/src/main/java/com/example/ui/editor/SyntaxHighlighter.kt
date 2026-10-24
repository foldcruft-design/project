package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.model.SyntaxTheme
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "def", "class", "import", "from", "return", "if", "else", "elif",
        "for", "while", "try", "except", "finally", "async", "await", "with",
        "as", "in", "is", "not", "and", "or", "lambda", "pass", "break",
        "continue", "raise", "yield", "None", "True", "False", "global", "nonlocal"
    )

    private val TOKEN_PATTERN = Pattern.compile(
        """(#.*)|(@\w+)|("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')|(\b\d+(?:\.\d+)?\b)|(\b\w+\b)|([+\-*/%=<>!&|^~:]+)"""
    )

    fun highlight(code: String, theme: SyntaxTheme): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            val lines = code.lines()
            var currentOffset = 0

            for (line in lines) {
                val matcher = TOKEN_PATTERN.matcher(line)
                while (matcher.find()) {
                    val start = currentOffset + matcher.start()
                    val end = currentOffset + matcher.end()

                    val comment = matcher.group(1)
                    val decorator = matcher.group(2)
                    val stringLit = matcher.group(3)
                    val number = matcher.group(4)
                    val word = matcher.group(5)
                    val operator = matcher.group(6)

                    when {
                        comment != null -> {
                            addStyle(
                                SpanStyle(color = theme.commentColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                start,
                                end
                            )
                        }
                        decorator != null -> {
                            addStyle(
                                SpanStyle(color = theme.decoratorColor, fontWeight = FontWeight.SemiBold),
                                start,
                                end
                            )
                        }
                        stringLit != null -> {
                            addStyle(
                                SpanStyle(color = theme.stringColor),
                                start,
                                end
                            )
                        }
                        number != null -> {
                            addStyle(
                                SpanStyle(color = theme.numberColor),
                                start,
                                end
                            )
                        }
                        word != null -> {
                            if (KEYWORDS.contains(word)) {
                                addStyle(
                                    SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold),
                                    start,
                                    end
                                )
                            } else {
                                // Check if next non-space char in line is '(' to color as function
                                val lineIndexAfterWord = matcher.end()
                                if (lineIndexAfterWord < line.length && line.substring(lineIndexAfterWord).trimStart().startsWith("(")) {
                                    addStyle(
                                        SpanStyle(color = theme.functionColor, fontWeight = FontWeight.Medium),
                                        start,
                                        end
                                    )
                                } else {
                                    addStyle(
                                        SpanStyle(color = theme.variableColor),
                                        start,
                                        end
                                    )
                                }
                            }
                        }
                        operator != null -> {
                            addStyle(
                                SpanStyle(color = theme.operatorColor),
                                start,
                                end
                            )
                        }
                    }
                }
                currentOffset += line.length + 1 // +1 for the newline
            }
        }
    }
}
