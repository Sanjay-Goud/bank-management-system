package com.sanjay.bms.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.sanjay.bms.dto.StatementRequest;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.entity.Transaction;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.repository.TransactionRepository;
import com.sanjay.bms.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class StatementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public byte[] generatePdfStatement(StatementRequest request, String username) {
        try {
            // Verify ownership
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!account.getUser().getId().equals(user.getId())) {
                throw new SecurityException("Access denied");
            }

            // Get transactions
            List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
                    request.getAccountId(), request.getStartDate(), request.getEndDate());

            // Create PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph header = new Paragraph("ACCOUNT STATEMENT", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            document.add(Chunk.NEWLINE);

            // Account details
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
            document.add(new Paragraph("Account Holder: " + account.getAccountHolderName(), normalFont));
            document.add(new Paragraph("Account Number: " + maskAccountNumber(account.getAccountNumber()), normalFont));
            document.add(new Paragraph("Account Type: " + account.getAccountType(), normalFont));
            document.add(new Paragraph("Statement Period: " +
                    formatDate(request.getStartDate()) + " to " + formatDate(request.getEndDate()), normalFont));
            document.add(Chunk.NEWLINE);

            // Transaction table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{2, 3, 2, 2, 2, 3});

            // Table headers
            addTableHeader(table, "Date");
            addTableHeader(table, "Description");
            addTableHeader(table, "Type");
            addTableHeader(table, "Amount");
            addTableHeader(table, "Balance");
            addTableHeader(table, "Reference");

            // Add transactions
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            for (Transaction txn : transactions) {
                addTableCell(table, txn.getTransactionDate().format(formatter));
                addTableCell(table, txn.getDescription());
                addTableCell(table, txn.getTransactionType());
                addTableCell(table, "₹" + txn.getAmount().toString());
                addTableCell(table, "₹" + txn.getBalanceAfter().toString());
                addTableCell(table, txn.getReferenceNumber());
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Summary
            BigDecimal totalDebit = transactions.stream()
                    .filter(t -> t.getTransactionType().contains("WITHDRAW") ||
                            t.getTransactionType().contains("TRANSFER_OUT"))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = transactions.stream()
                    .filter(t -> t.getTransactionType().contains("DEPOSIT") ||
                            t.getTransactionType().contains("TRANSFER_IN"))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            document.add(new Paragraph("Total Debits: ₹" + totalDebit, normalFont));
            document.add(new Paragraph("Total Credits: ₹" + totalCredit, normalFont));
            document.add(new Paragraph("Current Balance: ₹" + account.getBalance(), normalFont));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF statement: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF statement", e);
        }
    }

    public String generateCsvStatement(StatementRequest request, String username) {
        try {
            // Verify ownership
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!account.getUser().getId().equals(user.getId())) {
                throw new SecurityException("Access denied");
            }

            // Get transactions
            List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(
                    request.getAccountId(), request.getStartDate(), request.getEndDate());

            // Create CSV
            StringWriter sw = new StringWriter();
            CSVPrinter csvPrinter = new CSVPrinter(sw, CSVFormat.DEFAULT
                    .withHeader("Date", "Description", "Type", "Amount", "Balance", "Reference"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

            for (Transaction txn : transactions) {
                csvPrinter.printRecord(
                        txn.getTransactionDate().format(formatter),
                        txn.getDescription(),
                        txn.getTransactionType(),
                        txn.getAmount().toString(),
                        txn.getBalanceAfter().toString(),
                        txn.getReferenceNumber()
                );
            }

            csvPrinter.flush();
            return sw.toString();

        } catch (Exception e) {
            log.error("Error generating CSV statement: {}", e.getMessage());
            throw new RuntimeException("Failed to generate CSV statement", e);
        }
    }

    private void addTableHeader(PdfPTable table, String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }

    private String formatDate(java.time.LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }
}