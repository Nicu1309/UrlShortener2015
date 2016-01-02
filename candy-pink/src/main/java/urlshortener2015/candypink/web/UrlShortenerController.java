package urlshortener2015.candypink.web;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import urlshortener2015.common.domain.Click;
import urlshortener2015.candypink.domain.ShortURL;
import urlshortener2015.common.repository.ClickRepository;
import urlshortener2015.candypink.repository.ShortURLRepository;

import com.google.common.hash.Hashing;
import javax.ws.rs.client.WebTarget;

@RestController
public class UrlShortenerController {

	private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);
	
	@Autowired
	protected ShortURLRepository shortURLRepository;

	@RequestMapping(value = "/{id:(?!link|index|login|signUp|profile|manage).*}", method = RequestMethod.GET)
	public ResponseEntity<?> redirectTo(@PathVariable String id, HttpServletRequest request) {
		logger.info("Requested redirection with hash " + id);
		ShortURL l = shortURLRepository.findByKey(id);
		if (l != null) {
			return createSuccessfulRedirectToResponse(l);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	protected String extractIP(HttpServletRequest request) {
		return request.getRemoteAddr();
	}

	protected ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
		HttpHeaders h = new HttpHeaders();
		h.setLocation(URI.create(l.getTarget()));
		return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
	}


	@RequestMapping(value = "/link", method = RequestMethod.POST)
	public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
			@RequestParam(value = "token", required = false) String token,
			@RequestParam(value = "sponsor", required = false) String sponsor,
			@RequestParam(value = "brand", required = false) String brand, HttpServletRequest request) {
		logger.info("Requested new short for uri " + url);
		Client client = ClientBuilder.newClient();
		Response response = client.target(url).request().get();
		// Url is reachable
		if (response.getStatus() == 200) {
			logger.info("Uri " + url + " is reachable");
			ShortURL su = createAndSaveIfValid(url, token, sponsor, brand, UUID
				.randomUUID().toString(), extractIP(request));
			if (su != null) {
				// Url requested is not safe
				if (su.getSafe() == false) {
					HttpHeaders h = new HttpHeaders();
					h.setLocation(su.getUri());
					return new ResponseEntity<>(su, h, HttpStatus.CREATED);
				// Url requested is safe
				} else {
					return null;
				}
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		// Url is not reachable
		else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	protected ShortURL createAndSaveIfValid(String url, String token, String sponsor,
			String brand, String owner, String ip) {
		UrlValidator urlValidator = new UrlValidator(new String[] { "http",
				"https" });
		if (urlValidator.isValid(url)) {
			String id = Hashing.murmur3_32()
					.hashString(url, StandardCharsets.UTF_8).toString();
			ShortURL su = new ShortURL(id, url,
					linkTo(
						methodOn(UrlShortenerController.class).redirectTo(
							id, null)).toUri(), token, sponsor,
							new Date(System.currentTimeMillis()),
							owner, HttpStatus.TEMPORARY_REDIRECT.value(),
							false, null,null,null, null, ip, null, null);
			// This checks if uri is malware
			if (su != null) {
				boolean spam = checkInternal(su);	
				if (!spam) {
					return shortURLRepository.save(su);	
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/*
	* This method checks an URI against the Google Safe Browsing API,
	* then it updates the database if needed.
	* According to Google's API, by making a GET request the URI sent
	* is checked and message is created with status code in response.
	* Status OK 200 means that uri is unsafe, and 204 indicates that is
	* safe. Other reponse status are caused by error. 
	*/
   public boolean checkInternal(ShortURL url){
		Client client = ClientBuilder.newClient();
		
		// Preparing URI to check 
		WebTarget target = client.target("https://sb-ssl.google.com/safebrowsing/api/lookup");
		WebTarget targetWithQueryParams = target.queryParam("key", "AIzaSyDI60aszp__CG9n4B3n3gd-YDEh-uowUwM");
		targetWithQueryParams = targetWithQueryParams.queryParam("client", "CandyShort");
		targetWithQueryParams = targetWithQueryParams.queryParam("appver","1.0");
		targetWithQueryParams = targetWithQueryParams.queryParam("pver","3.1");
		targetWithQueryParams = targetWithQueryParams.queryParam("url",URLEncoder.encode(url.getTarget()));

		Response response = targetWithQueryParams.request(MediaType.TEXT_PLAIN_TYPE).get();
		ShortURL res;
		if (response.getStatus()==204) { 		// Uri is safe
			logger.info("La uri no es malware | no deseada");
			return false;
		} else if (response.getStatus()==200) { 	// Uri is unsafe
			logger.info("La uri es malware o no deseada");
			return true;
		}
		return false;
	}
}
