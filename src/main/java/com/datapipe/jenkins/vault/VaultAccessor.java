package com.datapipe.jenkins.vault;

import java.io.Serializable;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.datapipe.jenkins.vault.credentials.VaultCredential;
import com.datapipe.jenkins.vault.exception.VaultPluginException;

public class VaultAccessor implements Serializable {
	private static final long serialVersionUID = 1L;

	private transient Vault vault;

    private transient VaultConfig config;

    public void init(String url) {
        try {
            config = new VaultConfig().address(url).build();
//            This is only for testing locally
//            SslConfig sslConfig = new SslConfig();
//            sslConfig.verify(false);
//            config.sslConfig(sslConfig);

            vault = new Vault(config);
        } catch (VaultException e) {
            throw new VaultPluginException("failed to connect to vault", e);
        }
    }

    public void auth(VaultCredential vaultCredential) {
        vault = vaultCredential.authorizeWithVault(vault, config);
    }

    public LogicalResponse read(String path) {
        LogicalResponse response;
        try {
            response =  vault.logical().read(path);
            vault.leases().renew(response.getLeaseId(), 10000);
        } catch (VaultException e) {
            throw new VaultPluginException("could not read from vault: " + e.getMessage() + " at path: " + path, e);
        }


        return response;
    }

    public VaultResponse revoke(String leaseId) {
        try {
            return vault.leases().revoke(leaseId);
        } catch (VaultException e) {
            throw new VaultPluginException("could not revoke vault lease (" + leaseId + "):" + e.getMessage());
        }
    }
}
