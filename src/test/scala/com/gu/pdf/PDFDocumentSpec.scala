package com.gu.pdf

import java.io.File.createTempFile
import java.io._
import java.nio.file.{Files, Paths}
import javax.imageio.ImageIO

import com.gu.pdf.PDFDocument._
import org.scalatest.{BeforeAndAfter, FlatSpec, OneInstancePerTest}

class PDFDocumentSpec extends FlatSpec with BeforeAndAfter with OneInstancePerTest {

  val document1 = loadPDF(getClass.getResource("/page1.pdf").getPath).right.get
  val document2 = loadPDF(getClass.getResource("/page2.pdf").getPath).right.get
  val document3 = loadPDF(getClass.getResource("/pages1and2.pdf").getPath).right.get

  after {
    document1.close()
    document2.close()
    document3.close()
  }

  "A non-present PDF file" should "throw an IOException with the message: " in {
    val filename = "pageX.pdf"
    val result = loadPDF(filename)
    assert(result.isLeft)
    assert(result.left.get.isInstanceOf[IOException])
    assert(result.left.get.asInstanceOf[IOException].getMessage equals s"$filename")
  }

  "A present non-PDF file" should "throw an IOException" in {
    val result = loadPDF("build.sbt")
    assert(result.isLeft)
    assert(result.left.get.isInstanceOf[IOException])
    assert(result.left.get.asInstanceOf[IOException].getMessage equals "Error: End-of-File, expected line")
  }

  "Save as" should "actually save a document to a file" in {
    val filename = createTempFile("savedDocument1", ".pdf").getAbsolutePath
    val bytesFromDocument1 = document1.asByteArray

    document1.saveAs(filename)

    val bytesFromSavedDocument1 = Files.readAllBytes(Paths.get(filename))
    assert(bytesFromDocument1 sameElements bytesFromSavedDocument1)
  }

  "Two PDFDocuments get stitched together" should "return a new PDFDocument with the right number of pages" in {
    val stitched = document1.append(Seq(document2))

    assert(stitched != document1)
    assert(stitched.getNumberOfPages equals document1.getNumberOfPages + document2.getNumberOfPages)

    stitched.close()
  }

  "Two PDFDocument get stitched together" should "return a saveable PDFDocument which has the right number of pages" in {
    val newPDFDocument = document1.append(Seq(document2))
    val filename = createTempFile("tempDocument1-deleteme", ".pdf").getAbsolutePath
    newPDFDocument.saveAs(filename)
    newPDFDocument.close()

    val fromDisk = loadPDF(filename).right.get
    assert(newPDFDocument.getNumberOfPages equals fromDisk.getNumberOfPages)
    fromDisk.close()
  }

  "A heading" should "be added to every page of a PDFDocument" in {
    val heading = "Guardian Weekly Digital Edition for Subscriber: paul.brown@guardian.co.uk"

    val newDocument = document3.addCustomHeading(heading)
    val textEither = newDocument.getText
    newDocument.close()
    assert(textEither.isRight)

    val parsedText = textEither.right.get.split("\n")
    val pagesContainingHeading = parsedText.count(_.contains(heading))

    assert(pagesContainingHeading > 0)
    assert(pagesContainingHeading equals document3.getNumberOfPages)
  }

  "Copy" should "return a copied object, not the original" in {
    val copyOfOriginal = document1.copy

    assert(copyOfOriginal != document1)
    assert(copyOfOriginal.asByteArray sameElements document1.asByteArray)

    copyOfOriginal.close()
  }

  "Get first page as image" should "return a buffered image with a seemingly valid size" in {
    val heightToWidthRatio = document1.getFirstPageHWRatio
    assert(heightToWidthRatio > 0f)
    val image = document1.getFirstPageAsImage
    assert(image.getHeight.toFloat / image.getWidth.toFloat == heightToWidthRatio)
  }

  "Save first page to PNG file" should "return true and file should be readable with correct size" in {
    val imageFile = createTempFile("tempImage1-deleteme", ".png")
    val worked = document1.saveFirstPageToPNGFile(imageFile.getAbsolutePath)

    assert(worked.isRight && worked.right.get)

    val loaded = ImageIO.read(imageFile)

    assert(loaded.getHeight > 0, loaded.getWidth > 0)

    val generated = document1.getFirstPageAsImage
    assert(loaded.getHeight == generated.getHeight && loaded.getWidth == generated.getWidth)
  }
}
