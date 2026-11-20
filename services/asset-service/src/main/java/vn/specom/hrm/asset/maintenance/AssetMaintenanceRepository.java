package vn.specom.hrm.asset.maintenance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, Long> {
    Page<AssetMaintenance> findByAssetIdOrderByStartDateDescIdDesc(Long assetId, Pageable pageable);

    Optional<AssetMaintenance> findByIdAndAssetId(Long id, Long assetId);

    boolean existsByAssetIdAndStatusAndIdNot(Long assetId, AssetMaintenance.EStatus status, Long id);
}
