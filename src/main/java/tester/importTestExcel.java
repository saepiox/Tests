package tester;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.seleniumhq.jetty9.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/*
this does a minimalist import from an excel file into a list of objects, then verifies this against import into the page.

 */
public class importTestExcel{ 
    
    public static void readFile(File afile) throws IOException {
        Map<Integer,String> stringMap = null;
        List<position> positions = new LinkedList<position>();
        if (!afile.exists()||(!FilenameUtils.getExtension(afile.getAbsolutePath()).equalsIgnoreCase("xlsx") && !FilenameUtils.getExtension(afile.getAbsolutePath()).equalsIgnoreCase("xls")) ){
            System.out.println("yes, no, pull the other one, it has bells on it. this needs to be an excelsheet");
        }
        else {
            AtomicInteger i = new AtomicInteger(), j = new AtomicInteger();

            Workbook anImportofPositions = WorkbookFactory.create(afile);

            anImportofPositions.forEach(sheet -> {
                i.set(0);
                sheet.forEach(row -> {
                i.getAndIncrement();
                System.out.println(i.intValue());
                j.set(-1);
                position aposition = new position();
                    row.forEach(cell -> {
                        j.getAndIncrement();
                        if (i.intValue() > 2){

                        switch (j.intValue()) {
                            case 0:

                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setPortfolioId(cell.getNumericCellValue());
                                }
                                break;

                            case 1:
                                if (cell.getCellType() == CellType.STRING) {

                                    aposition.setPositionId((cell.getStringCellValue()));
                                }
                                break;
                            case 3:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setPositionName(cell.getStringCellValue());

                                }
                                break;
                            case 4:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setPositionNominal(cell.getNumericCellValue());
                                }
                                break;
                            case 5:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setExposureRatio(((byte) cell.getNumericCellValue()));
                                }
                                break;
                            case 6:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setPositionGroup(cell.getStringCellValue());
                                }
                                break;
                            case 7:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setPositionType(cell.getStringCellValue());
                                }
                                break;
                            case 8:
                                if (cell.getCellType() == CellType.STRING) {
                                }
                                break;
                            case 9:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setLegNo(cell.getStringCellValue());
                                }
                                break;
                            case 10:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setAllocation(cell.getStringCellValue());
                                }
                                break;
                            case 11:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setExposureCurrency(cell.getStringCellValue());
                                }
                                break;
                            case 12:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setCalcWeight(cell.getNumericCellValue());
                                }
                                break;
                            case 13:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setPfDepth(((int) cell.getNumericCellValue()));
                                }
                                break;
                            case 14:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setWeightedMarketValueExpCCY(String.valueOf(cell.getNumericCellValue()));
                                }
                                break;
                            case 15:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setWeightedExposureValueExpCCY(String.valueOf(cell.getNumericCellValue()));
                                }
                                break;
                            case 16:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setName(cell.getStringCellValue());
                                }
                                break;
                            case 17:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setBasCurrency(cell.getStringCellValue());
                                }
                                break;
                            case 18:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setParent((byte) cell.getNumericCellValue());
                                }
                                break;
                            case 19:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setExpCcyPair(cell.getStringCellValue());
                                }
                                break;
                            case 20:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setExpCcyRate((float) cell.getNumericCellValue());
                                }
                                break;
                            case 21:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setPrice((float) cell.getNumericCellValue());
                                }
                                break;
                            case 22:
                                if (cell.getCellType() == CellType.STRING) {
                                    aposition.setPriceDate(correctingDateFormat(cell.getStringCellValue()));
                                }
                                break;
                            case 23:
                                if (cell.getCellType() == CellType.NUMERIC) {
                                    aposition.setIsReportingDay(correctingDateFormat(String.valueOf(cell.getNumericCellValue())));
                                }
                                break;
                            case 24:
                                if (cell.getCellType() == CellType.NUMERIC) {

                                    aposition.setHedgeable(String.valueOf(cell.getNumericCellValue()));
                                }
                            default:

                        }};


                });
System.out.println("");
    positions.add(aposition);
            });
                System.out.println(positions.size());
            });}}
    private static String removingDoubleQuotations(String aNumber3) {
        return aNumber3.replaceAll("\"", "").trim().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");

    }
    private static String correctingDateFormat(String aNumber5) {

        if (aNumber5.length() < 10) {
            return "9999-11-11";
        } else if (StringUtil.isBlank(aNumber5.substring(1, 2))) {
            return "9999-11-11";


        } else {
            return aNumber5.substring(7, 11) + "-" + aNumber5.substring(4, 6) + "-" + aNumber5.substring(1, 3).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");


        }
    }
}