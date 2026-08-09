package com.daemon.expnesso.ui.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    minFontSize: TextUnit = 10.sp,
    style: TextStyle = LocalTextStyle.current
) {
    val defaultFontSize = if (fontSize.isUnspecified) style.fontSize else fontSize
    val targetFontSize = if (defaultFontSize.isUnspecified) 16.sp else defaultFontSize
    
    val textMeasurer = rememberTextMeasurer()
    
    BoxWithConstraints(modifier = modifier) {
        var currentFontSize = targetFontSize
        val maxWidth = constraints.maxWidth
        
        var hasCalculated = false
        while (!hasCalculated && currentFontSize > minFontSize) {
            val mergedStyle = style.merge(
                TextStyle(
                    color = color,
                    fontSize = currentFontSize,
                    fontWeight = fontWeight,
                    textAlign = textAlign ?: TextAlign.Unspecified,
                    lineHeight = lineHeight,
                    fontFamily = fontFamily,
                    textDecoration = textDecoration,
                    fontStyle = fontStyle,
                    letterSpacing = letterSpacing
                )
            )

            val textLayoutResult = textMeasurer.measure(
                text = text,
                style = mergedStyle,
                maxLines = maxLines,
                softWrap = softWrap
            )
            
            if (textLayoutResult.size.width > maxWidth) {
                val nextSize = (currentFontSize.value * 0.9f).sp
                if (nextSize > minFontSize) {
                    currentFontSize = nextSize
                } else {
                    currentFontSize = minFontSize
                    hasCalculated = true
                }
            } else {
                hasCalculated = true
            }
        }
        
        Text(
            text = text,
            color = color,
            fontSize = currentFontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            style = style
        )
    }
}

@Composable
fun AutoSizeText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    minFontSize: TextUnit = 10.sp,
    style: TextStyle = LocalTextStyle.current
) {
    val defaultFontSize = if (fontSize.isUnspecified) style.fontSize else fontSize
    val targetFontSize = if (defaultFontSize.isUnspecified) 16.sp else defaultFontSize
    
    val textMeasurer = rememberTextMeasurer()
    
    BoxWithConstraints(modifier = modifier) {
        var currentFontSize = targetFontSize
        val maxWidth = constraints.maxWidth
        
        var hasCalculated = false
        while (!hasCalculated && currentFontSize > minFontSize) {
            val mergedStyle = style.merge(
                TextStyle(
                    color = color,
                    fontSize = currentFontSize,
                    fontWeight = fontWeight,
                    textAlign = textAlign ?: TextAlign.Unspecified,
                    lineHeight = lineHeight,
                    fontFamily = fontFamily,
                    textDecoration = textDecoration,
                    fontStyle = fontStyle,
                    letterSpacing = letterSpacing
                )
            )

            val textLayoutResult = textMeasurer.measure(
                text = text,
                style = mergedStyle,
                maxLines = maxLines,
                softWrap = softWrap
            )
            
            if (textLayoutResult.size.width > maxWidth) {
                val nextSize = (currentFontSize.value * 0.9f).sp
                if (nextSize > minFontSize) {
                    currentFontSize = nextSize
                } else {
                    currentFontSize = minFontSize
                    hasCalculated = true
                }
            } else {
                hasCalculated = true
            }
        }
        
        Text(
            text = text,
            color = color,
            fontSize = currentFontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            style = style
        )
    }
}
