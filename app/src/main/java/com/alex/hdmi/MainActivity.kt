package com.alex.hdmi

import android.annotation.SuppressLint
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.alex.hdmi.databinding.ActivityMainBinding
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import kotlin.math.abs

class MainActivity : AppCompatActivity(), ICameraHelper.StateCallback {

    private lateinit var binding: ActivityMainBinding
    private var cameraHelper: CameraHelper? = null

    private var isSurfaceCreated = false

    // 沉浸式显示状态标记
    private var isImmersive = false

    // 悬浮按钮拖动参数
    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private val CLICK_ACTION_THRESHOLD = 200
    private val MOVE_THRESHOLD = 10f
    private var startClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 允许内容绘制到刘海屏/挖孔屏区域（消除顶部黑边）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initCameraHelper()
        initFabTouchAndDrag()
    }

    private fun initCameraHelper() {
        cameraHelper = CameraHelper().apply {
            setStateCallback(this@MainActivity)
        }

        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d("MainActivity", "Surface created")
                isSurfaceCreated = true
                // 解决权限未主动触发问题：主动检测当前 USB 设备
                checkAndSelectDevice()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d("MainActivity", "Surface destroyed")
                isSurfaceCreated = false
                cameraHelper?.removeSurface(holder.surface)
            }
        })
    }

    /**
     * 主动检查已连接的 USB 采集卡并发起权限授权
     */
    private fun checkAndSelectDevice() {
        val deviceList = cameraHelper?.deviceList
        if (!deviceList.isNullOrEmpty()) {
            val device = deviceList[0]
            Log.d("MainActivity", "Found USB device: ${device.deviceName}")
            cameraHelper?.selectDevice(device)
        } else {
            Log.d("MainActivity", "No USB camera device found")
        }
    }

    /**
     * 悬浮按钮拖拽 + 点击切换沉浸式模式
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initFabTouchAndDrag() {
        binding.fabFullScreen.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startClickTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    val parent = view.parent as View
                    val maxX = (parent.width - view.width).toFloat()
                    val maxY = (parent.height - view.height).toFloat()

                    view.x = newX.coerceIn(0f, maxX)
                    view.y = newY.coerceIn(0f, maxY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val upTime = System.currentTimeMillis()
                    val deltaX = abs(event.rawX - downRawX)
                    val deltaY = abs(event.rawY - downRawY)

                    if (deltaX < MOVE_THRESHOLD && deltaY < MOVE_THRESHOLD && (upTime - startClickTime) < CLICK_ACTION_THRESHOLD) {
                        view.performClick()
                        toggleImmersiveMode()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 切换沉浸式显示模式（全屏隐藏/显示状态栏和导航栏）
     */
    private fun toggleImmersiveMode() {
        isImmersive = !isImmersive

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isImmersive) {
            // 开启沉浸式模式：隐藏状态栏和导航栏
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            Toast.makeText(this, "开启沉浸式显示", Toast.LENGTH_SHORT).show()
        } else {
            // 关闭沉浸式模式：显示状态栏和导航栏
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            Toast.makeText(this, "关闭沉浸式显示", Toast.LENGTH_SHORT).show()
        }
    }

    // --- ICameraHelper.StateCallback 回调 ---

    override fun onAttach(device: UsbDevice?) {
        Log.d("MainActivity", "USB device attached: ${device?.deviceName}")
        cameraHelper?.selectDevice(device)
    }

    override fun onDetach(device: UsbDevice?) {
        Log.d("MainActivity", "USB device detached")
        cameraHelper?.closeCamera()
    }

    override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {
        Log.d("MainActivity", "Device opened: ${device?.deviceName}")
        cameraHelper?.openCamera()
    }

    override fun onDeviceClose(device: UsbDevice?) {
        Log.d("MainActivity", "Device closed")
    }

    override fun onCameraOpen(device: UsbDevice?) {
        Log.d("MainActivity", "Camera opened successfully")

        // 获取支持的 Size 列表
        val sizeList = cameraHelper?.supportedSizeList

        // 匹配 1080P 分辨率，若没有则取第一个默认分辨率
        val targetSize = sizeList?.find { it.width == 1920 && it.height == 1080 }
            ?: sizeList?.firstOrNull()

        targetSize?.let {
            cameraHelper?.setPreviewSize(it)
        }

        binding.surfaceView.setAspectRatio(16, 9)

        if (isSurfaceCreated) {
            cameraHelper?.addSurface(binding.surfaceView.holder.surface, false)
            cameraHelper?.startPreview()
        }
    }

    override fun onCameraClose(device: UsbDevice?) {
        Log.d("MainActivity", "Camera closed")
        binding.surfaceView.holder.surface?.let {
            cameraHelper?.removeSurface(it)
        }
    }

    override fun onCancel(device: UsbDevice?) {
        Toast.makeText(this, "需要 USB 权限才能使用采集卡", Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        if (isSurfaceCreated) {
            checkAndSelectDevice()
        }
    }

    override fun onStop() {
        super.onStop()
        cameraHelper?.stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper?.closeCamera()
        cameraHelper?.release()
        cameraHelper = null
    }
}