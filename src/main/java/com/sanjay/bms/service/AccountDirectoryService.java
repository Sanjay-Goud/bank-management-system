package com.sanjay.bms.service;

import com.sanjay.bms.dto.AccountDirectoryDto;
import java.util.List;

public interface AccountDirectoryService {
    List<AccountDirectoryDto> searchAccounts(String query);
    AccountDirectoryDto getAccountByNumber(String accountNumber);
    List<AccountDirectoryDto> getAllActiveAccounts();

    List<AccountDirectoryDto> getAllAccountsDirectory();

    AccountDirectoryDto validateAccountForTransfer(String accountNumber);
}