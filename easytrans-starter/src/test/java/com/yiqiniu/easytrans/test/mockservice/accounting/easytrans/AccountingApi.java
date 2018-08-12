package com.yiqiniu.easytrans.test.mockservice.accounting.easytrans;

import java.util.concurrent.Future;

import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingRequest;
import com.yiqiniu.easytrans.test.mockservice.accounting.easytrans.AccountingCpsMethod.AccountingResponse;

public interface AccountingApi {
	Future<AccountingResponse> accounting(AccountingRequest request);
}
