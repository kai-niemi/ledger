package se.cockroachdb.ledger.repository.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import se.cockroachdb.ledger.ProfileNames;
import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.repository.TransferRepository;

@Repository
@Profile(ProfileNames.JPA)
public class JpaTransferRepositoryAdapter implements TransferRepository {
    @Autowired
    private TransferJpaRepository transferJpaRepository;

    @Autowired
    private TransferItemJpaRepository transferItemJpaRepository;

    @Override
    public Transfer createTransfer(Transfer transfer) {
        return transferJpaRepository.save(transfer);
    }

    @Override
    public List<TransferItem> createTransferItems(List<TransferItem> items) {
        return items;
    }

    @Override
    public Transfer findTransferById(UUID transferId) {
        return transferJpaRepository.findById(transferId).orElse(null);
    }

    @Override
    public boolean checkTransferExists(UUID transferId, String city) {
        return transferJpaRepository.existsById(transferId);
    }

    @Override
    public Page<Transfer> findAllTransfersByAccountId(UUID accountId, Pageable pageable) {
        return transferJpaRepository.findAll(accountId, pageable);
    }

    @Override
    public Page<Transfer> findAllTransfersByCity(String city, Pageable pageable) {
        return transferJpaRepository.findAllByCity(city, pageable);
    }

    @Override
    public Page<Transfer> findAllTransfers(Pageable pageable) {
        return transferJpaRepository.findAll(pageable);
    }

    @Override
    public Page<TransferItem> findAllTransferItems(UUID transferId, Pageable pageable) {
        return transferItemJpaRepository.findById(transferId, pageable);
    }

    @Override
    public void deleteAll() {
        transferItemJpaRepository.deleteAllInBatch();
        transferJpaRepository.deleteAllInBatch();
    }
}
