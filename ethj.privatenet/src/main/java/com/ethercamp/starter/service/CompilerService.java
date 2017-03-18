package com.ethercamp.starter.service;

import org.ethereum.solidity.compiler.CompilationResult;

import java.io.IOException;

/**
 * Created by blainemalone on 11/03/2017.
 */
public interface CompilerService {

    CompilationResult.ContractMetadata compileContract() throws IOException;

}
