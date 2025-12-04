package com.central.wallet_service.grpc;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcWalletServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {
}
