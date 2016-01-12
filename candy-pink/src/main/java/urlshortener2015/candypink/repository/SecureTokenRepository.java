package urlshortener2015.candypink.repository;

import java.util.List;

import urlshortener2015.candypink.domain.SecureToken;

public interface SecureTokenRepository {

	SecureToken findByToken(String token);

	SecureToken save(SecureToken token);

	void delete(String token);
}