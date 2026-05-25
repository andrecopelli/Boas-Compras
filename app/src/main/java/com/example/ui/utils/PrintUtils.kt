package com.example.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.database.ShoppingListItem
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintUtils {

    // Print list using WebView printing (generates highly detailed, multi-page, customizable invoice look)
    fun printShoppingList(
        context: Context,
        listName: String,
        items: List<ShoppingListItem>,
        totalAmount: Double
    ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        val formattedTotal = formatCurrency(totalAmount)

        val html = StringBuilder().apply {
            append("<!DOCTYPE html><html><head>")
            append("<meta charset='utf-8'>")
            append("<style>")
            append("body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #1D1B20; padding: 20px; line-height: 1.4; }")
            append(".header { border-bottom: 2px solid #6750A4; padding-bottom: 12px; margin-bottom: 20px; }")
            append(".title { font-size: 24px; font-weight: bold; color: #6750A4; margin: 0; text-transform: uppercase; }")
            append(".subtitle { font-size: 11px; color: #666; margin: 4px 0 0 0; letter-spacing: 0.5px; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 15px; }")
            append("th { background-color: #F3EDF7; color: #6750A4; text-align: left; padding: 10px; font-size: 12px; font-weight: bold; border-bottom: 1px solid #CAC4D0; }")
            append("td { padding: 10px; font-size: 13px; border-bottom: 1px solid #E0E0E0; }")
            append(".zebra { background-color: #F7F2FA; }")
            append(".item-name { font-weight: bold; }")
            append(".right { text-align: right; }")
            append(".footer { margin-top: 30px; border-top: 2px solid #6750A4; padding-top: 15px; display: flex; justify-content: space-between; align-items: center; }")
            append(".total-label { font-size: 12px; font-weight: bold; color: #666; text-transform: uppercase; }")
            append(".total-amount { font-size: 24px; font-weight: 900; color: #1D1B20; }")
            append("</style></head><body>")

            append("<div class='header'>")
            append("<div class='title'>Boas Compras</div>")
            append("<div class='subtitle'>Lista: $listName &bull; Criada em: $formattedDate &bull; por André Copelli</div>")
            append("</div>")

            append("<table>")
            append("<thead><tr><th>PRODUTO</th><th>QTD x PREÇO UNIT.</th><th class='right'>SUBTOTAL</th></tr></thead>")
            append("<tbody>")

            items.forEachIndexed { idx, item ->
                val trClass = if (idx % 2 == 1) "class='zebra'" else ""
                append("<tr $trClass>")
                append("<td><span class='item-name'>${item.name}</span></td>")
                append("<td>${formatQuantity(item.quantity)} x ${formatCurrency(item.unitPrice)}</td>")
                append("<td class='right'>${formatCurrency(item.totalPrice)}</td>")
                append("</tr>")
            }

            append("</tbody>")
            append("</table>")

            append("<div class='footer'>")
            append("<div>")
            append("<div class='total-label'>Total da Compra</div>")
            append("<div class='total-amount'>$formattedTotal</div>")
            append("</div>")
            append("<div style='text-align: right; font-size: 10px; color: #999; margin-top: 10px;'>")
            append("Gerado no app Boas Compras")
            append("</div>")
            append("</div>")

            append("</body></html>")
        }.toString()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "Boas Compras - $listName"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    // Direct PDF write to a shareable local document for native PDF export
    fun exportListAsPdf(
        context: Context,
        listName: String,
        items: List<ShoppingListItem>,
        totalAmount: Double
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Paper size in points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        // Draw title
        var yPosition = 50f
        paint.color = Color.parseColor("#6750A4")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 22f
        paint.isAntiAlias = true
        canvas.drawText("BOAS COMPRAS", 40f, yPosition, paint)
        yPosition += 20f

        // Author
        paint.color = Color.DKGRAY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 10f
        canvas.drawText("Lista: $listName • por André Copelli", 40f, yPosition, paint)
        yPosition += 10f

        // Date
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Data de Geração: ${sdf.format(Date())}", 40f, yPosition, paint)
        yPosition += 25f

        // Draw headers line
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#CAC4D0")
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 15f

        // Table headers text
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#6750A4")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("PRODUTO", 45f, yPosition, paint)
        canvas.drawText("QTD x UNITÁRIO", 280f, yPosition, paint)
        canvas.drawText("SUBTOTAL", 480f, yPosition, paint)
        yPosition += 8f

        // Draw line below headers
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#CAC4D0")
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 20f

        // Items logic
        paint.style = Paint.Style.FILL
        items.forEachIndexed { index, item ->
            // Zebra striping backgrounds
            if (index % 2 == 1) {
                val stripePaint = Paint().apply {
                    color = Color.parseColor("#F7F2FA")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(40f, yPosition - 14f, 555f, yPosition + 6f, stripePaint)
            }

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#1D1B20")
            canvas.drawText(item.name.take(32), 45f, yPosition, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.DKGRAY
            canvas.drawText(
                "${formatQuantity(item.quantity)} x ${formatCurrency(item.unitPrice)}",
                280f,
                yPosition,
                textPaint
            )

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.parseColor("#6750A4")
            canvas.drawText(formatCurrency(item.totalPrice), 480f, yPosition, textPaint)

            yPosition += 22f

            // Add new page if content overflows height
            if (yPosition > 780f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
        }

        // Divider before totally amount
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#6750A4")
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 24f

        // Draw final sum
        paint.style = Paint.Style.FILL
        paint.color = Color.DKGRAY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL DA COMPRA", 45f, yPosition, paint)
        yPosition += 18f

        paint.color = Color.parseColor("#1D1B20")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatCurrency(totalAmount), 45f, yPosition, paint)

        // Footer note
        paint.color = Color.LTGRAY
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Gerado por Boas Compras no Android", 400f, yPosition, paint)

        pdfDocument.finishPage(page)

        try {
            val fileName = "boas_compras_${listName.replace(" ", "_").replace(":", "-")}.pdf"
            val file = File(context.cacheDir, fileName)
            val stream = FileOutputStream(file)
            pdfDocument.writeTo(stream)
            pdfDocument.close()
            stream.close()

            // Share/Send via standard action selector
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Lista de Compras: $listName")
                putExtra(Intent.EXTRA_TEXT, "Compartilhando minha lista de compras via app Boas Compras!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Salvar/Enviar PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao exportar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            format.format(amount)
        } catch (e: Exception) {
            val formatted = String.format(Locale.US, "%.2f", amount).replace(".", ",")
            "R$ $formatted"
        }
    }

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).replace(".", ",")
        }
    }
}
