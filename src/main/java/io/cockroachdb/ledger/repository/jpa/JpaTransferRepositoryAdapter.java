package io.cockroachdb.ledger.repository.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import io.cockroachdb.ledger.ProfileNames;
import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.domain.TransferItemEntity;
import io.cockroachdb.ledger.domain.TransferType;
import io.cockroachdb.ledger.repository.TransferRepository;

@Repository
@Profile(ProfileNames.JPA)
public class JpaTransferRepositoryAdapter implements TransferRepository {
    @Autowired
    private TransferJpaRepository transferJpaRepository;

    @Autowired
    private TransferItemJpaRepository transferItemJpaRepository;

    @Override
    public TransferEntity createTransfer(TransferEntity transferEntity) {
        return transferJpaRepository.save(transferEntity);
    }

    @Override
    public List<TransferItemEntity> createTransferItems(List<TransferItemEntity> items) {
        return items;
    }

    @Override
    public TransferEntity findTransferById(UUID transferId) {
        return transferJpaRepository.findById(transferId).orElse(null);
    }

    @Override
    public boolean checkTransferExists(UUID transferId) {
        return transferJpaRepository.existsById(transferId);
    }

    @Override
    public Page<TransferEntity> findAllTransfersByAccountId(UUID accountId, Pageable pageable) {
        return transferJpaRepository.findAll(accountId, pageable);
    }

    @Override
    public Page<TransferEntity> findAllTransfersByCity(String city, Pageable pageable) {
        return transferJpaRepository.findAllByCity(city, pageable);
    }

    @Override
    public Page<TransferEntity> findAllTransfers(TransferType transferType, Pageable pageable) {
        return transferJpaRepository.findAll(transferType, pageable);
    }

    @Override
    public Page<TransferItemEntity> findAllTransferItems(UUID transferId, Pageable pageable) {
        return transferItemJpaRepository.findById(transferId, pageable);
    }

    @Override
    public void deleteAll() {
        transferItemJpaRepository.deleteAllInBatch();
        transferJpaRepository.deleteAllInBatch();
    }
}
