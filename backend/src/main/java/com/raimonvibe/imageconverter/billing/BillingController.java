package com.raimonvibe.imageconverter.billing;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

  @Value("${app.stripe.secretKey:}")
  private String stripeSecretKey;

  @Value("${app.stripe.priceUsd:1.0}")
  private Double priceUsd;

  @Value("${app.stripe.pricePackSize:5}")
  private Integer packSize;

  @PostMapping("/checkout")
  public ResponseEntity<Map<String, Object>> createCheckout(@RequestParam("successUrl") String successUrl, @RequestParam("cancelUrl") String cancelUrl, java.security.Principal principal) throws Exception {
    Stripe.apiKey = stripeSecretKey;
    long priceCents = Math.round(priceUsd * 100);
    SessionCreateParams.Builder builder =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            .setUnitAmount(priceCents)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(packSize + " extra conversions")
                                .build())
                            .build())
                    .build())
            .putMetadata("packSize", packSize.toString());
    if (principal != null) {
      builder.setCustomerEmail(principal.getName());
    }
    Session session = Session.create(builder.build());
    return ResponseEntity.ok(Map.of("id", session.getId(), "url", session.getUrl()));
  }
}
