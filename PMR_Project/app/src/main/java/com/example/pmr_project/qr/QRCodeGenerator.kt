package com.example.pmr_project.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.example.pmr_project.data.entities.Part

object QRCodeGenerator {
    
    fun generateQRCode(content: String, size: Int = 512): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1)
        }
        
        val bits = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    fun generateQRCodeForPart(part: Part, size: Int = 512): Bitmap {
        val content = part.qrCode
        return generateQRCode(content, size)
    }
    
    fun generateAllDemoQRCodes(): Map<String, Bitmap> {
        val qrCodes = mutableMapOf<String, Bitmap>()
        
        val demoParts = listOf(
            "FILTRE_AIR_001",
            "PLAQUETTE_FREIN_002", 
            "HUILE_MOTEUR_003",
            "BOUGIE_004",
            "AMORTISSEUR_005",
            "RADIATEUR_006",
            "ALTERNATEUR_007",
            "DEMARREUR_008"
        )
        
        demoParts.forEach { qrCode ->
            qrCodes[qrCode] = generateQRCode(qrCode, 256)
        }
        
        return qrCodes
    }
} 