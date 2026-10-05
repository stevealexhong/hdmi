package com.alex.hdmi

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class UsbPermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 当插上采集卡时，启动主界面
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // 传递 USB 挂载事件参数
            if (getIntent().hasExtra(UsbManager.EXTRA_DEVICE)) {
                putExtra(UsbManager.EXTRA_DEVICE, getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE) as android.hardware.usb.UsbDevice?)
            }
        }
        startActivity(intent)
        finish()
    }
}