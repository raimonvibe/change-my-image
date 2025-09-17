package com.raimonvibe.imageconverter.billing;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/billing")
@Validated
public class BillingController {

  @Value("${app.stripe.secretKey:}")
  private String stripeSecretKey;

  @Value("${app.stripe.priceUsd:1.98}")
  private double priceUsd; // $ per pack, removed duplicate declaration

  @Value("${app.stripe.pricePackSize:5}")
  private int packSize;    // conversions per pack

  @Value("${app.stripe.currency:usd}")
  private String currency;

  // Added fields for successUrl and cancelUrl to fix uninitialized variable issue
  @Value("${app.stripe.successUrl:http://localhost:8080/success}")
  private String successUrl;

  @Value("${app.stripe.cancelUrl:http://localhost:8080/cancel}")
  private String cancelUrl;

  @PostConstruct
  void initStripe() {
    if (!StringUtils.hasText(stripeSecretKey)) {
      throw new IllegalStateException("Stripe secret key is not configured (app.stripe.secretKey).");
    }
    if (priceUsd <= 0.0) {
      throw new IllegalStateException("Price must be > 0 (app.stripe.priceUsd).");
    }
    if (packSize < 1) {
      throw new IllegalStateException("Pack size must be >= 1 (app.stripe.pricePackSize).");
    }
    if (!StringUtils.hasText(currency)) {
      currency = "usd";
    }
    Stripe.apiKey = stripeSecretKey;
  }

  @PostMapping("/checkout")
  public ResponseEntity<?> createCheckout(
      @RequestParam("successUrl") @NotBlank String successUrl,
      @RequestParam("cancelUrl") @NotBlank String cancelUrl,
      Principal principal,
      HttpServletRequest request
  ) {
    try {
      // Validate absolute http(s) URLs to avoid open redirects / invalid callbacks
      String validatedSuccess = validateAbsoluteHttpUrl(successUrl);
      String validatedCancel = validateAbsoluteHttpUrl(cancelUrl);

      long unitAmountCents = Math.round(priceUsd * 100.0);

      SessionCreateParams.LineItem.PriceData.ProductData product =
          SessionCreateParams.LineItem.PriceData.ProductData.builder()
              .setName("Unlimited Image Conversions")
              .build();

      SessionCreateParams.LineItem.PriceData priceData =
          SessionCreateParams.LineItem.PriceData.builder()
              .setCurrency(currency.toLowerCase())
              .setUnitAmount(unitAmountCents)
              .setProductData(product)
              .setRecurring(
                  SessionCreateParams.LineItem.PriceData.Recurring.builder()
                      .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                      .build()
              )
              .build();

      SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
          .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
          .setSuccessUrl(validatedSuccess)
          .setCancelUrl(validatedCancel)
          .addLineItem(
              SessionCreateParams.LineItem.builder()
                  .setQuantity(1L)
                  .setPriceData(priceData)
                  .build()
          )
          .putMetadata("subscription", "unlimited_conversions");

      // Optionally add customer email if authenticated principal is present
      if (principal != null && StringUtils.hasText(principal.getName())) {
        paramsBuilder.setCustomerEmail(principal.getName());
      }

      SessionCreateParams params = paramsBuilder.build();

      Session session = Session.create(params);

      return ResponseEntity.ok(new CheckoutResponse(session.getId(), session.getUrl()));
    } catch (IllegalArgumentException | URISyntaxException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(error("invalid_request", e.getMessage()));
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(error("stripe_error", safeStripeMessage(e)));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(error("server_error", "Unexpected error creating checkout session."));
    }
  }

  // ---- Helpers ----

  private static String validateAbsoluteHttpUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
      throw new URISyntaxException(url, "URL must start with http:// or https://");
    }
    if (uri.getHost() == null) {
      throw new URISyntaxException(url, "URL must contain a host");
    }
    return uri.toString();
  }

  private static Map<String, Object> error(String code, String message) {
    return Map.of("error", Map.of("code", code, "message", message));
  }

  private static String safeStripeMessage(StripeException e) {
    // Prevent leaking internal details
    String msg = e.getMessage();
    return (msg != null && !msg.isEmpty()) ? msg : "Stripe API call failed.";
  }

  // Response DTO
  public record CheckoutResponse(String id, String url) {}
}
