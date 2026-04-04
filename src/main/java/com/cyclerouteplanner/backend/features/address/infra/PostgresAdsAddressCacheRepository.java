package com.cyclerouteplanner.backend.features.address.infra;

import com.cyclerouteplanner.backend.features.address.domain.AdsAddressCacheEntry;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressCachePort;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class PostgresAdsAddressCacheRepository implements AdsAddressCachePort {

    private final JdbcTemplate jdbcTemplate;

    public PostgresAdsAddressCacheRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(AdsAddressCacheEntry entry) {
        jdbcTemplate.update(
                """
                insert into address.ads_address_cache (
                    ads_oid,
                    full_address,
                    normalized_address,
                    etak_code,
                    location,
                    raw_payload
                )
                values (
                    ?,
                    ?,
                    ?,
                    ?,
                    case
                        when ? is not null and ? is not null
                            then ST_SetSRID(ST_MakePoint(?, ?), 4326)
                        else null
                    end,
                    cast(? as jsonb)
                )
                on conflict (ads_oid) do update set
                    full_address = excluded.full_address,
                    normalized_address = excluded.normalized_address,
                    etak_code = excluded.etak_code,
                    location = excluded.location,
                    raw_payload = excluded.raw_payload,
                    updated_at = now()
                """,
                entry.adsOid(),
                entry.fullAddress(),
                entry.normalizedAddress(),
                entry.etakCode(),
                entry.longitude(),
                entry.latitude(),
                entry.longitude(),
                entry.latitude(),
                entry.rawPayload()
        );
    }
}
