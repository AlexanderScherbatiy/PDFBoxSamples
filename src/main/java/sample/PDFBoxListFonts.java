package sample;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PDFBoxListFonts {

    private final Map<PDFont, Font> fontsCache = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.printf("usage: java %s <input pdf file>%n", PDFBoxListFonts.class.getName());
            System.exit(1);
        }

        String file = args[0];
        System.out.printf("pdf file: %s%n", file);

        new PDFBoxListFonts().listFonts(file);
    }

    private void listFonts(String pdfFile) throws IOException {
        PDDocument doc = PDDocument.load(new File(pdfFile));
        for (int i = 0; i < doc.getNumberOfPages(); ++i) {
            PDPage page = doc.getPage(i);
            PDResources res = page.getResources();
            for (COSName fontName : res.getFontNames()) {
                PDFont pdFont = res.getFont(fontName);

                Font awtFont = getAwtFont(pdFont);
                if (awtFont == null) {
                    continue;
                }

                String fontFamily = awtFont.getFamily();
                System.out.printf("%s [font family is %s]%n", awtFont, fontFamily.isEmpty() ? "empty" : "set");

                awtFont.canDisplay(1);
                if (!fontFamily.equals(awtFont.getFamily())) {
                    System.out.printf("The font can't display some characters! It is substituted to %s by AWT system.%n", awtFont);
                }
            }
        }
    }

    private Font getAwtFont(PDFont pdFont) throws IOException {

        Font awtFont = fontsCache.get(pdFont);

        if (awtFont != null) {
            return awtFont;
        }

        if (pdFont instanceof PDType0Font) {
            return cacheFont(pdFont, getPDType0AwtFont((PDType0Font) pdFont));
        }

        if (pdFont instanceof PDType1Font) {
            return cacheFont(pdFont, getPDType1AwtFont((PDType1Font) pdFont));
        }

        String msg = String.format("Not yet implemented: %s", pdFont.getClass().getName());
        throw new UnsupportedOperationException(msg);
    }

    public Font getPDType0AwtFont(PDType0Font pdFont) throws IOException {

        PDCIDFont descendantFont = pdFont.getDescendantFont();

        if (descendantFont != null) {
            if (descendantFont instanceof PDCIDFontType2) {
                return getPDCIDAwtFontType2((PDCIDFontType2) descendantFont);
            }
        }

        return null;
    }

    private Font getPDType1AwtFont(PDType1Font pdFont) throws IOException {

        PDFontDescriptor fd = pdFont.getFontDescriptor();

        if (fd != null) {
            if (fd.getFontFile() != null) {
                try {
                    // create a type1 font with the embedded data
                    return Font.createFont(Font.TYPE1_FONT, fd.getFontFile().createInputStream());
                } catch (FontFormatException e) {
                    err("Can't read the embedded type1 font " + fd.getFontName());
                }
            }
        }

        return null;
    }

    public Font getPDCIDAwtFontType2(PDCIDFontType2 pdFont) throws IOException {

        PDFontDescriptor fd = pdFont.getFontDescriptor();
        PDStream ff2Stream = fd.getFontFile2();

        if (ff2Stream != null) {
            try {
                // create a font with the embedded data
                return Font.createFont(Font.TRUETYPE_FONT, ff2Stream.createInputStream());
            } catch (FontFormatException f) {
                err("Can't read the embedded font " + fd.getFontName());
            }
        }

        return null;
    }

    private Font cacheFont(PDFont pdFont, final Font awtFont) {
        fontsCache.put(pdFont, awtFont);
        return awtFont;
    }

    private void err(String str) {
        System.err.printf("err: %s%n", str);
    }
}
