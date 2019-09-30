package uk.co.jatra.numberwheel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

/*

Basic idea:
 - digit can be replaced by another sliding in from top
 - only ever one digit's worth visible, but maybe be fraction of one, fraction of next.
 - Current value known.
 - next value obv knowable
 */


/*
Want attributes for
 - how many digits should be shown above (fractional)
 - how many digits should be shown below (fractional)
 - size of font
 - colour of font
 - style of font
 - speed
 - rest position (when stopped, where is whole digit shown)
 - start interpolator (nullable)
 - stop interpolator (nullable)
 - background

 - how many digits
 - whether a decimal point is shown
 - postion of decimal point
 - whether numbers count or independent
 - whether numbers all rotate same position
 - start number
 - end number
 - infinite spin or from start to end
 - rotation speed
 - rotation direction
 */
private const val TAG = "NumberWheel"

class CounterWheel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var startTime: Long = 0
    private var animators: Array<ValueAnimator>
    private var digits: IntArray
    private var yTranslations: FloatArray
    private var running: Boolean = false
    private var numberOfDigits: Int = 1
    var fillColour = Color.CYAN
    var animationDuration = 1000
    var isFilled = true
        set(value) {
            field = value
            invalidate()
        }
    private var numberHeight = 100f

    private val textPaint by lazy {
        Paint().apply {
            textSize = numberHeight
            color = Color.BLACK
        }
    }
    private val numberWidth by lazy {
        textPaint.measureText("8")
    }
    private val paddingLeftF: Float
        get() = paddingLeft.toFloat()
    private val paddingRightF: Float
        get() = paddingRight.toFloat()
    private val paddingTopF: Float
        get() = paddingTop.toFloat()
    private val paddingBottomF: Float
        get() = paddingBottom.toFloat()

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NumberWheel,
            0, 0
        ).apply {
            try {
                numberHeight = getDimension(R.styleable.NumberWheel_textSize, 16f)
                numberOfDigits = getInteger(R.styleable.NumberWheel_numDigits, 1)
                animationDuration = getInteger(R.styleable.NumberWheel_duration, 1000)
                fillColour = getColor(R.styleable.NumberWheel_fillColour, Color.CYAN)
                isFilled = getBoolean(R.styleable.NumberWheel_fillBg, true)
                yTranslations = FloatArray(numberOfDigits)
                digits = IntArray(numberOfDigits)
                animators = Array(numberOfDigits, ::createAnimator)
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        count()
    }

    private fun count() {
        var delay = 100L
        handler.postDelayed({
            changeDigit(0)
            count()
        }, delay)
    }

    private fun createAnimator(i: Int): ValueAnimator {
        return ValueAnimator.ofFloat(0f, numberHeight).apply {
            duration = animationDuration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                yTranslations[i] = animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    digits[i] = (digits[i] + 1) % 10
                    if (true && running) {
                        animators[i].start()
                        if (digits[i] == 0 && i < numberOfDigits - 1) changeDigit(i + 1)
                    } else {
                        yTranslations[i] = 0f
                        animators[i].cancel()
                    }
                }

            })
        }
    }

    fun start() {
        startTime = System.nanoTime()
        running = true
        animators[0].start()

//        animateDigit(0, true)
//        animateDigit(1, true)
    }

    private fun animateDigit(whichDigit: Int, repeat: Boolean) {
//        animator = ValueAnimator.ofFloat(0f, numberHeight).apply {
//            duration = animationDuration.toLong()
//            interpolator = LinearInterpolator()
//            addUpdateListener {
//                yTranslations[whichDigit] = animatedValue as Float
//                invalidate()
//            }
//            addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator?) {
//                    digits[whichDigit] = (digits[whichDigit] + 1) % 10
//                    if (repeat && running) {
//                        animator.start()
//                    } else {
//                        yTranslations[whichDigit] = 0f
//                        animator.cancel()
//                    }
//                }
//
//            })
//        }
        animators[whichDigit].start()
    }

    fun changeDigit(i: Int) {
        val animator = ValueAnimator.ofFloat(0f, numberHeight).apply {
            duration = animationDuration.toLong()
            interpolator = LinearInterpolator()
            addUpdateListener {
                yTranslations[i] = animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    digits[i] = (digits[i] + 1) % 10
                    yTranslations[i] = 0f
                    if (digits[i] == 0 && i < numberOfDigits-1) changeDigit(i+1)
                }
            })
        }
        animator.start()
    }

    fun stop() {
        running = false
    }

    fun restart() {
        stop()
        start()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth: Int = numberWidth.toInt() * numberOfDigits
        val w: Int = resolveSizeAndState(minWidth, widthMeasureSpec, 1)

        val minHeight: Int = numberHeight.toInt()
        val h: Int = resolveSizeAndState(minHeight, heightMeasureSpec, 1)
        setMeasuredDimension(w + paddingLeft + paddingRight, h + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas?) {

        if (isFilled) canvas?.drawColor(fillColour)
        canvas?.clipRect(
            paddingLeftF,
            paddingTopF,
            width - paddingRightF,
            height.toFloat() - paddingBottomF
        )


        (0 until numberOfDigits).forEach { drawDigit(canvas, it, yTranslations[it]) }
//        drawDigit(canvas, 0, yTranslations[0])
//        drawDigit(canvas, 1, yTranslations[1])
    }

    private fun drawDigit(
        canvas: Canvas?,
        position: Int,
        verticalTranslation: Float
    ) {
        val baseLine = numberHeight + verticalTranslation
        val indent = paddingLeftF + (numberOfDigits - position - 1) * numberWidth

        canvas?.drawText(
            digits[position].toString(),
            indent,
            paddingTopF + baseLine,
            textPaint
        )

        canvas?.drawText(
            ((digits[position] + 1) % 10).toString(),
            indent,
            paddingTopF + baseLine - numberHeight,
            textPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return super.onTouchEvent(event)
    }
}