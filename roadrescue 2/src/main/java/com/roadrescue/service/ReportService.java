package com.roadrescue.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.roadrescue.entity.BreakdownRequest;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReportService {

    public void generateRequestReport(BreakdownRequest request,
                                      OutputStream outputStream) throws Exception {

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // HEADER
        Table headerTable = new Table(new float[]{1, 3});
        headerTable.setWidth(UnitValue.createPercentValue(100));

        // LOGO
        try {
            String logoPath = System.getProperty("user.dir")
                    + "/src/main/resources/static/images/logo.png";

            ImageData logoData = ImageDataFactory.create(logoPath);
            Image logo = new Image(logoData);
            logo.setWidth(80);
            logo.setHeight(50);

            Cell logoCell = new Cell().add(logo)
                    .setBorder(Border.NO_BORDER);

            headerTable.addCell(logoCell);

        } catch (Exception e) {
            headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));
        }

        // TITLE
        Paragraph title = new Paragraph("ROAD RESCUE SERVICE REPORT")
                .setBold()
                .setFontSize(18)
                .setTextAlignment(TextAlignment.RIGHT);

        Cell titleCell = new Cell().add(title)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // REQUEST INFO TABLE
        Table infoTable = new Table(new float[]{2, 4});
        infoTable.setWidth(UnitValue.createPercentValue(100));

        addRow(infoTable, "Request ID", String.valueOf(request.getId()));
        addRow(infoTable, "Status", String.valueOf(request.getStatus()));

        if (request.getCreatedAt() != null) {
            addRow(infoTable, "Created",
                    request.getCreatedAt().format(formatter));
        }

        document.add(new Paragraph("REQUEST INFORMATION")
                .setBold()
                .setFontSize(12));

        document.add(infoTable);
        document.add(new Paragraph("\n"));

        // CUSTOMER TABLE
        document.add(new Paragraph("CUSTOMER DETAILS")
                .setBold()
                .setFontSize(12));

        Table customerTable = new Table(new float[]{2, 4});
        customerTable.setWidth(UnitValue.createPercentValue(100));

        if (request.getUser() != null) {
            addRow(customerTable, "Name", request.getUser().getFullName());
            addRow(customerTable, "Phone", request.getUser().getPhone());
            addRow(customerTable, "Email", request.getUser().getEmail());
        }

        document.add(customerTable);
        document.add(new Paragraph("\n"));

        // VEHICLE TABLE

        document.add(new Paragraph("VEHICLE DETAILS")
                .setBold()
                .setFontSize(12));

        Table vehicleTable = new Table(new float[]{2, 4});
        vehicleTable.setWidth(UnitValue.createPercentValue(100));

        if (request.getVehicle() != null) {
            addRow(vehicleTable, "Plate", request.getVehicle().getPlateNumber());
            addRow(vehicleTable, "Brand", request.getVehicle().getBrand());
            addRow(vehicleTable, "Model", request.getVehicle().getModel());
            addRow(vehicleTable, "Type", request.getVehicle().getVehicleType());
            addRow(vehicleTable, "Year", String.valueOf(request.getVehicle().getYear()));
            addRow(vehicleTable, "Color", request.getVehicle().getColor());
        }

        document.add(vehicleTable);

        // VEHICLE IMAGE (CENTER)

        if (request.getVehicle() != null &&
                request.getVehicle().getFrontImagePath() != null) {

            try {
                String imgPath = System.getProperty("user.dir")
                        + "/" + request.getVehicle().getFrontImagePath();

                ImageData imgData = ImageDataFactory.create(imgPath);
                Image img = new Image(imgData);

                img.setWidth(250);
                img.setHeight(160);
                img.setHorizontalAlignment(HorizontalAlignment.CENTER);

                document.add(new Paragraph("\nVEHICLE IMAGE")
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER));

                document.add(img);

            } catch (Exception e) {
                document.add(new Paragraph("Vehicle image not available"));
            }
        }

        document.add(new Paragraph("\n"));

        // BREAKDOWN TABLE

        document.add(new Paragraph("BREAKDOWN DETAILS")
                .setBold()
                .setFontSize(12));

        Table breakdownTable = new Table(new float[]{2, 4});
        breakdownTable.setWidth(UnitValue.createPercentValue(100));

        addRow(breakdownTable, "Description",
                request.getDescription() != null ? request.getDescription() : "N/A");

        addRow(breakdownTable, "Latitude",
                request.getLatitude() != null ? request.getLatitude().toString() : "N/A");

        addRow(breakdownTable, "Longitude",
                request.getLongitude() != null ? request.getLongitude().toString() : "N/A");

        document.add(breakdownTable);
        document.add(new Paragraph("\n"));

        // GARAGE TABLE

        if (request.getGarage() != null) {

            document.add(new Paragraph("ASSIGNED GARAGE")
                    .setBold()
                    .setFontSize(12));

            Table garageTable = new Table(new float[]{2, 4});
            garageTable.setWidth(UnitValue.createPercentValue(100));

            addRow(garageTable, "Garage", request.getGarage().getGarageName());
            addRow(garageTable, "Phone", request.getGarage().getPhone());
            addRow(garageTable, "Address", request.getGarage().getAddress());

            document.add(garageTable);
        }

        // FOOTER

        document.add(new Paragraph("\n\nThank you for using RoadRescue 🚗")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(12)
                .setFontColor(ColorConstants.GRAY));

        document.close();
        outputStream.flush();
    }

    // helper method (clean code)
    private void addRow(Table table, String key, String value) {

        Cell c1 = new Cell()
                .add(new Paragraph(key))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold();

        Cell c2 = new Cell()
                .add(new Paragraph(value != null ? value : "N/A"));

        table.addCell(c1);
        table.addCell(c2);
    }
}