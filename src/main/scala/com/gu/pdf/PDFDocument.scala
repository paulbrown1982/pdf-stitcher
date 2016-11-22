package com.gu.pdf

import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File}
import java.nio.file.{Files, Paths}
import javax.imageio.ImageIO

import com.gu.pdf.PDFDocument.tryToEither
import org.apache.pdfbox.pdmodel.PDDocument._
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode._
import org.apache.pdfbox.pdmodel.font.PDType1Font._
import org.apache.pdfbox.pdmodel.{PDDocument, PDPageContentStream}
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper

import scala.collection.JavaConverters._
import scala.util.{Either, Failure, Left, Right, Success, Try}

class PDFDocument(protected val underlying: PDDocument) {

  def asByteArray: Array[Byte] = {
    val outputStream = new ByteArrayOutputStream(1024)
    underlying.save(outputStream)
    outputStream.toByteArray
  }

  def copy: PDFDocument = new PDFDocument(load(this.asByteArray))

  def writeTo(file: File): Option[Throwable] = {
    Try(underlying.save(file)) match {
      case Success(something) => None
      case Failure(err) => Some(err)
    }
  }

  def saveAs(filename: String): Option[Throwable] = {
    Try(underlying.save(filename)) match {
      case Success(something) => None
      case Failure(err) => Some(err)
    }
  }

  def close(): Unit = underlying.close()

  def getText: Either[Throwable, String] = {
    tryToEither(Try(PDFDocument.pdfStripper.getText(underlying)))
  }

  def getNumberOfPages = underlying.getNumberOfPages

  def append(rest: Seq[PDFDocument]): PDFDocument = {
    val newDocument = this.copy
    rest.foreach { next =>
      val nextPages = next.underlying.getDocumentCatalog.getPages.asScala
      nextPages.foreach(newDocument.underlying.addPage)
    }
    newDocument
  }

  def addCustomHeading(heading: String): PDFDocument = {
    val leading = 22
    val newDocument = this.copy
    val pages = newDocument.underlying.getPages.asScala.toSeq
    pages.foreach { page =>
      val contentStream = new PDPageContentStream(newDocument.underlying, page, APPEND, true, true)
      contentStream.setFont(TIMES_ROMAN, 14)
      contentStream.setNonStrokingColor(0)
      contentStream.beginText()
      contentStream.setLeading(leading)
      contentStream.newLineAtOffset(12, page.getCropBox.getHeight - leading)
      contentStream.showText(heading)
      contentStream.endText()
      contentStream.close()
    }
    newDocument
  }

  def getFirstPageAsImage: BufferedImage = {
    val renderer = new PDFRenderer(underlying)
    renderer.renderImage(0)
  }

  def saveFirstPageToPNGFile(filename: String): Either[Throwable, Boolean] = {
    tryToEither(Try {
      val file = new File(filename)
      ImageIO.write(getFirstPageAsImage, "png", file)
    })
  }

  def getFirstPageHWRatio: Float = {
    val cropBox = underlying.getPage(0).getCropBox
    cropBox.getHeight / cropBox.getWidth
  }
}

object PDFDocument {
  private val pdfStripper = new PDFTextStripper()

  private def tryToEither[A](obj: Try[A]): Either[Throwable, A] = {
    obj match {
      case Success(something) => Right(something)
      case Failure(err) => Left(err)
    }
  }

  def loadPDF(filename: String): Either[Throwable, PDFDocument] = {
    tryToEither(Try {
      val bytes = Files.readAllBytes(Paths.get(filename))
      new PDFDocument(load(bytes))
    })
  }

}
