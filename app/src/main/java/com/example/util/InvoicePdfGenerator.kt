package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Invoice
import java.io.File
import java.io.FileOutputStream

object InvoicePdfGenerator {

    /**
     * Standard A5 page size in PostScript points: 148mm x 210mm
     * 1 mm = 2.834 points => 148 * 2.834 ≈ 420 pt width, 210 * 2.834 ≈ 595 pt height
     */
    private const val A5_WIDTH = 420
    private const val A5_HEIGHT = 595

    fun generateA5InvoicePdf(context: Context, invoice: Invoice): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A5_WIDTH, A5_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints for styling
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B365D") // Deep Navy
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 7f
            isAntiAlias = true
        }

        val boldLabelPaint = Paint().apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8f
            isAntiAlias = true
        }

        val regularLabelPaint = Paint().apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8f
            isAntiAlias = true
        }

        val italicSubtitlePaint = Paint().apply {
            color = Color.parseColor("#1B365D")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textSize = 7.5f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val primaryLinePaint = Paint().apply {
            color = Color.parseColor("#1B365D")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#1B365D")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 7.5f
            isAntiAlias = true
        }

        val tableCellPaint = Paint().apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 7f
            isAntiAlias = true
        }

        val warrantyTitlePaint = Paint().apply {
            color = Color.parseColor("#C0392B") // Mild Red / Coral Deep
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 7f
            isAntiAlias = true
        }

        val warrantyBodyPaint = Paint().apply {
            color = Color.parseColor("#2C3E50")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 6f
            isAntiAlias = true
        }

        val bgPaint = Paint().apply {
            color = Color.parseColor("#F5F7FA") // Ambient light background fill
            style = Paint.Style.FILL
        }

        // --- DRAWING HEADER ---
        // Top colorful accent bar
        canvas.drawRect(20f, 15f, 400f, 18f, Paint().apply {
            color = Color.parseColor("#1B365D")
            style = Paint.Style.FILL
        })

        // Shop Title
        canvas.drawText("SHREE RAM TRADER", 20f, 35f, titlePaint)
        
        // Shop Address details
        canvas.drawText("📍 Dindli Market, Sabji Market Lin, Near Sulabh Sauchalya", 20f, 46f, subtitlePaint)
        canvas.drawText("Jamshedpur, Jharkhand - Pincode: 831013", 20f, 54f, subtitlePaint)
        
        // Horizontal divider line
        canvas.drawLine(20f, 62f, 400f, 62f, primaryLinePaint)

        // --- DRAWING BILL INFO ---
        // Left Column (Bill No, Customer details)
        canvas.drawText("BILL NO: SRT-${invoice.invoiceNumber}", 20f, 75f, boldLabelPaint)
        canvas.drawText("DATE: ${invoice.dateString}", 300f, 75f, regularLabelPaint)

        // Draw centered payment status stamp on the PDF
        val statusText = if (invoice.isPaid) invoice.paymentStatus.uppercase() else "PENDING"
        val stampPaint = Paint().apply {
            color = if (invoice.isPaid) Color.parseColor("#107C41") else Color.parseColor("#C0392B")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 6.5f
            isAntiAlias = true
        }
        val stampBgPaint = Paint().apply {
            color = if (invoice.isPaid) Color.parseColor("#E8F8F5") else Color.parseColor("#FDEDEC")
            style = Paint.Style.FILL
        }
        val stampBorderPaint = Paint().apply {
            color = if (invoice.isPaid) Color.parseColor("#107C41") else Color.parseColor("#C0392B")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        
        val rectLeft = 160f
        val rectRight = 265f
        val textWidth = stampPaint.measureText(statusText)
        val textX = rectLeft + (rectRight - rectLeft - textWidth) / 2
        
        canvas.drawRoundRect(rectLeft, 67f, rectRight, 77f, 2f, 2f, stampBgPaint)
        canvas.drawRoundRect(rectLeft, 67f, rectRight, 77f, 2f, 2f, stampBorderPaint)
        canvas.drawText(statusText, textX, 74.5f, stampPaint)

        canvas.drawText("CUSTOMER NAME:", 20f, 90f, boldLabelPaint)
        canvas.drawText(invoice.customerName.uppercase(), 105f, 90f, regularLabelPaint)
        
        canvas.drawText("MOBILE NUMBER:", 20f, 102f, boldLabelPaint)
        canvas.drawText(if (invoice.customerMobile.isBlank()) "N/A" else invoice.customerMobile, 105f, 102f, regularLabelPaint)

        canvas.drawText("DELIVERY ADDRESS:", 20f, 114f, boldLabelPaint)
        // Make sure address doesn't overflow, simple truncate if very long
        val addressText = if (invoice.customerAddress.isBlank()) "Jamshedpur, Jharkhand" else invoice.customerAddress
        val truncatedAddress = if (addressText.length > 55) addressText.substring(0, 52) + "..." else addressText
        canvas.drawText(truncatedAddress, 105f, 114f, regularLabelPaint)

        canvas.drawLine(20f, 122f, 400f, 122f, linePaint)

        // --- DRAWING TABLE HEADER ---
        // Fill table header background
        canvas.drawRect(20f, 128f, 400f, 142f, bgPaint)
        canvas.drawText("S.N.", 23f, 138f, tableHeaderPaint)
        canvas.drawText("DESCRIPTION OF GOODS", 50f, 138f, tableHeaderPaint)
        canvas.drawText("BRAND", 185f, 138f, tableHeaderPaint)
        canvas.drawText("QTY", 255f, 138f, tableHeaderPaint)
        canvas.drawText("RATE (₹)", 295f, 138f, tableHeaderPaint)
        canvas.drawText("TOTAL (₹)", 350f, 138f, tableHeaderPaint)
        
        canvas.drawLine(20f, 142f, 400f, 142f, primaryLinePaint)

        // --- DRAWING TABLE ROWS ---
        var currentY = 155f
        invoice.items.forEachIndexed { index, item ->
            val snStr = (index + 1).toString()
            canvas.drawText(snStr, 23f, currentY, tableCellPaint)
            
            // Description wrapping logic or truncation to prevent columns overlapping
            val desc = if (item.itemName.length > 27) item.itemName.substring(0, 25) + ".." else item.itemName
            canvas.drawText(desc, 50f, currentY, tableCellPaint)
            
            val brand = if (item.brandName.isBlank()) "Local" else item.brandName
            canvas.drawText(brand, 185f, currentY, tableCellPaint)
            
            canvas.drawText(item.quantity.toString(), 258f, currentY, tableCellPaint)
            canvas.drawText(String.format("%,.2f", item.rate), 295f, currentY, tableCellPaint)
            canvas.drawText(String.format("%,.2f", item.totalPrice), 350f, currentY, tableCellPaint)

            canvas.drawLine(20f, currentY + 5f, 400f, currentY + 5f, Paint().apply {
                color = Color.parseColor("#EAECEE")
                strokeWidth = 0.5f
            })
            currentY += 16f
        }

        // Space below the items table
        currentY += 10f

        // --- GRAND TOTAL ROW ---
        canvas.drawRect(200f, currentY - 12f, 400f, currentY + 8f, bgPaint)
        canvas.drawLine(200f, currentY - 12f, 400f, currentY - 12f, linePaint)
        canvas.drawText("GRAND TOTAL:", 210f, currentY, boldLabelPaint)
        canvas.drawText(String.format("₹ %,.2f", invoice.grandTotal), 335f, currentY, Paint().apply {
            color = Color.parseColor("#1B365D")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9.5f
            isAntiAlias = true
        })
        canvas.drawLine(200f, currentY + 8f, 400f, currentY + 8f, linePaint)

        // Move to footer details
        currentY += 30f

        // --- WARRANTY BOX ---
        // Draw elegant warranty warning box
        val boxTop = currentY
        val boxBottom = currentY + 70f
        canvas.drawRect(20f, boxTop, 400f, boxBottom, bgPaint)
        canvas.drawRect(20f, boxTop, 400f, boxBottom, Paint().apply {
            color = Color.parseColor("#BDC3C7")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        })
        // Highlight border left with red color
        canvas.drawRect(20f, boxTop, 24f, boxBottom, Paint().apply {
            color = Color.parseColor("#C0392B")
            style = Paint.Style.FILL
        })

        canvas.drawText("📋 WARRANTY TERMS & CONDITIONS (APPLIED STRICTLY)", 30f, boxTop + 14f, warrantyTitlePaint)
        
        // Split warranty text into displayable lines
        val infoLines = splitWarrantyTerms(invoice)
        var lineY = boxTop + 26f
        infoLines.forEach { lineText ->
            canvas.drawText(lineText, 30f, lineY, warrantyBodyPaint)
            lineY += 10f
        }

        // --- CLOSING MESSAGE FOOTER ---
        canvas.drawText("Thank you for shopping with us! Safely securing your workplace.", 210f, 545f, italicSubtitlePaint.apply {
            textAlign = Paint.Align.CENTER
        })

        // Draw small copyright/branding of app helper
        canvas.drawText("Digital Bill Generated Safely via SRT Digital Billing App", 210f, 558f, subtitlePaint.apply {
            textAlign = Paint.Align.CENTER
            color = Color.GRAY
        })

        pdfDocument.finishPage(page)

        // Save file to application cache directory for sharing
        val dir = File(context.cacheDir, "invoices")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "SRT_Bill_${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        return file
    }

    private fun splitWarrantyTerms(invoice: Invoice): List<String> {
        val lines = mutableListOf<String>()
        
        // Analyze the invoice brand warranty profile
        val brands = invoice.items.map { it.brandName.lowercase().trim() }.distinct()
        val hasBrandedShoes = brands.any { it == "acme" || it == "bata" || it == "image" || it == "hillson" }
        val hasLocalOrOthers = brands.any { it == "local" || it.isBlank() } || invoice.items.any { !it.itemName.lowercase().contains("shoe") }

        if (hasBrandedShoes) {
            val brandedNames = invoice.items
                .filter { it.brandName.lowercase().trim() in listOf("acme", "bata", "image", "hillson") }
                .map { "${it.brandName} (${it.itemName})" }
                .distinct()
                .joinToString(", ")
            
            lines.add("• Branded Products: $brandedNames carry 3 Months Warranty against manufacturing defects.")
            lines.add("  Warranty starts from the purchase date (${invoice.dateString}). Subject to verification standard.")
        } else {
            lines.add("• Branded Shoes: Acme, Bata, Image, Hillson qualify for 3 Months Warranty.")
        }

        if (hasLocalOrOthers) {
            lines.add("• Non-Branded / Other Items: Local shoes, safety goggles, gloves & accessories have NO WARRANTY.")
        }
        
        lines.add("• To claim warranty (where applicable), please present this physical copy or WhatsApp digital invoice.")
        lines.add("• Note: Warranty claims do not cover normal wear and tear, cuts, or burning damage.")

        return lines
    }
}
