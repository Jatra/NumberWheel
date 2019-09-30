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
private const val TAG = "TimerWheel"

interface TimerListener {
    fun update(value: Int)
}

class TimerWheel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var paused: Boolean = false
    var listener: TimerListener? = null
    private var startTime: Long = 0
    private var digits: IntArray
    private var isAnimating: BooleanArray
    private var yTranslations: FloatArray
    private var running: Boolean = false
    private var numberOfDigits: Int = 1
    var fillColour = Color.CYAN
    var animationDuration = 1000
    var aboveBelow = 0.2f
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
    private val lineHeight by lazy {
        textPaint.fontMetrics.leading - textPaint.fontMetrics.top
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
                isAnimating = BooleanArray(numberOfDigits)
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTime = System.currentTimeMillis()
        tick()
    }

    private fun tick() {
        if (paused) return
        val delay = 10L
        handler.postDelayed({
            val elapsed = System.currentTimeMillis() - startTime
            listener?.update(elapsed.toInt())
            var divisor = 100
            for (digit in 0 until numberOfDigits) {
                val value = (elapsed / divisor) % 10
                changeDigit(digit, value.toInt())
                divisor *= 10
            }
            tick()
        }, delay)
    }

    fun start() {
        running = true
        tick()
    }


    private fun changeDigit(i: Int, newValue: Int) {
        val currentValue = digits[i]
        if (isAnimating[i] || newValue == currentValue) {
            return
        }
        isAnimating[i] = true
        var durationDividend = newValue - currentValue
        if (durationDividend < 0) {
            durationDividend = 1
        }
        val animator = ValueAnimator.ofFloat(0f, numberHeight).apply {
            duration = animationDuration.toLong() / (durationDividend)
                interpolator = LinearInterpolator()
            addUpdateListener {
                yTranslations[i] = animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    digits[i] = (digits[i] + 1) % 10
                    yTranslations[i] = 0f
                    isAnimating[i] = false
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
            paddingTopF + baseLine - lineHeight,
            textPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (paused) {
            paused = false
            tick()
        } else {
            paused = true
        }
        return super.onTouchEvent(event)
    }
}