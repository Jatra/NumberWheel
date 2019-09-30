package uk.co.jatra.numberwheel

import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var startAt: Long = 0
    private var handler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        durationBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                counterWheel.animationDuration = progress * 100
                counterWheel.restart()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        aboveBelow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                counterWheel.aboveBelow = progress / 100f
                counterWheel.requestLayout()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        fill.setOnCheckedChangeListener { buttonView, isChecked -> counterWheel.isFilled = isChecked }

        counterWheel.listener = object: TimerListener {
            override fun update(value: Int) {
                count.text = "$value"
            }

        }

        startButton.setOnClickListener {
            counterWheel.start()
            startButton.isEnabled = false
            stopButton.isEnabled = true
        }
        stopButton.setOnClickListener {
            counterWheel.stop()
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
        startAt = System.currentTimeMillis()
        updateClock()
    }

    private fun updateClock() {
        val deciseconds = ((System.currentTimeMillis() - startAt)/100).toInt()
        clock.text = "$deciseconds"
        handler.postDelayed({
            updateClock()
        }, 100)
    }
}
