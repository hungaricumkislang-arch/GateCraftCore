package com.gatecraft.billing;

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.OnResumeListener;
import com.google.appinventor.components.runtime.collect.Lists;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DesignerComponent(
    version = 1,
    description = "GateCraft Google Play Billing bridge using Play Billing Library 9.1.0. One-time products only in prototype v1.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "com.android.vending.BILLING")
@UsesLibraries(libraries = "__GCB_LIBS__")
public final class GateCraftBilling extends AndroidNonvisibleComponent
    implements PurchasesUpdatedListener, OnResumeListener, OnDestroyListener {

  private final Activity activity;
  private final Form form;
  private BillingClient billingClient;
  private boolean initializeRequested = false;
  private boolean connecting = false;
  private final Map<String, ProductDetails> productCache = new HashMap<String, ProductDetails>();
  private final Set<String> ownedProducts = new HashSet<String>();
  private String lastError = "";
  private int lastResponseCode = BillingClient.BillingResponseCode.OK;

  public GateCraftBilling(ComponentContainer container) {
    super(container.$form());
    form = container.$form();
    activity = container.$context();
    form.registerForOnResume(this);
    form.registerForOnDestroy(this);
  }

  @SimpleFunction(description = "Initializes Google Play Billing and connects to the Play Store. Safe to call repeatedly.")
  public void Initialize() {
    initializeRequested = true;
    ensureClient();
    connectIfNeeded();
  }

  @SimpleFunction(description = "Returns true when the BillingClient connection is ready.")
  public boolean IsReady() {
    return billingClient != null && billingClient.isReady();
  }

  @SimpleFunction(description = "Returns Google Play Billing Library version used by this extension.")
  public String BillingLibraryVersion() {
    return "9.1.0";
  }

  @SimpleFunction(description = "Queries comma-separated one-time product IDs. Result is delivered through ProductsLoaded.")
  public void QueryProducts(String productIdsCsv) {
    final List<String> ids = parseIds(productIdsCsv);
    if (ids.size() == 0) {
      fail("QueryProducts", BillingClient.BillingResponseCode.DEVELOPER_ERROR, "No product IDs supplied.");
      return;
    }
    withReadyClient(new ReadyAction() {
      @Override public void run() {
        queryProductDetails(ids, false, "");
      }
    });
  }

  @SimpleFunction(description = "Launches purchase flow for one one-time product. Product details are refreshed immediately before purchase.")
  public void Purchase(final String productId) {
    final String id = clean(productId);
    if (id.length() == 0) {
      fail("Purchase", BillingClient.BillingResponseCode.DEVELOPER_ERROR, "Product ID is empty.");
      return;
    }
    withReadyClient(new ReadyAction() {
      @Override public void run() {
        List<String> one = new ArrayList<String>();
        one.add(id);
        queryProductDetails(one, true, id);
      }
    });
  }

  @SimpleFunction(description = "Queries current one-time purchases and refreshes local entitlement state.")
  public void RefreshEntitlements() {
    withReadyClient(new ReadyAction() {
      @Override public void run() {
        queryOwnedPurchases();
      }
    });
  }

  @SimpleFunction(description = "Alias for RefreshEntitlements. Useful for a Restore purchases button.")
  public void RestorePurchases() {
    RefreshEntitlements();
  }

  @SimpleFunction(description = "Returns true when the latest purchase refresh contains a PURCHASED entitlement for this product ID.")
  public boolean HasEntitlement(String productId) {
    return ownedProducts.contains(clean(productId));
  }

  @SimpleFunction(description = "Returns cached localized product price, or empty string until QueryProducts has completed.")
  public String GetProductPrice(String productId) {
    ProductDetails pd = productCache.get(clean(productId));
    ProductDetails.OneTimePurchaseOfferDetails offer = chooseOffer(pd);
    return offer == null ? "" : safe(offer.getFormattedPrice());
  }

  @SimpleFunction(description = "Returns cached product name, or empty string until QueryProducts has completed.")
  public String GetProductName(String productId) {
    ProductDetails pd = productCache.get(clean(productId));
    return pd == null ? "" : safe(pd.getName());
  }

  @SimpleFunction(description = "Returns cached product description, or empty string until QueryProducts has completed.")
  public String GetProductDescription(String productId) {
    ProductDetails pd = productCache.get(clean(productId));
    return pd == null ? "" : safe(pd.getDescription());
  }

  @SimpleFunction(description = "Returns the last Billing response code seen by the extension.")
  public int LastResponseCode() {
    return lastResponseCode;
  }

  @SimpleFunction(description = "Returns the last Billing error/debug message seen by the extension.")
  public String LastError() {
    return lastError;
  }

  @SimpleEvent(description = "Billing connection completed successfully.")
  public void BillingReady() {
    EventDispatcher.dispatchEvent(this, "BillingReady");
  }

  @SimpleEvent(description = "Billing connection was lost. Auto service reconnection remains enabled.")
  public void BillingDisconnected() {
    EventDispatcher.dispatchEvent(this, "BillingDisconnected");
  }

  @SimpleEvent(description = "Product query completed. Products is a list of records [id,name,description,formattedPrice,currency,priceMicros,offerToken].")
  public void ProductsLoaded(YailList products) {
    EventDispatcher.dispatchEvent(this, "ProductsLoaded", products);
  }

  @SimpleEvent(description = "Google Play purchase UI was launched for the requested product.")
  public void PurchaseStarted(String productId) {
    EventDispatcher.dispatchEvent(this, "PurchaseStarted", productId);
  }

  @SimpleEvent(description = "A purchase is still pending. No entitlement is granted yet.")
  public void PurchasePending(String productId, String purchaseToken) {
    EventDispatcher.dispatchEvent(this, "PurchasePending", productId, purchaseToken);
  }

  @SimpleEvent(description = "A PURCHASED one-time product was detected. Acknowledgement is completed before this event is emitted when acknowledgement was required.")
  public void PurchaseCompleted(String productId, String purchaseToken, boolean acknowledged) {
    EventDispatcher.dispatchEvent(this, "PurchaseCompleted", productId, purchaseToken, acknowledged);
  }

  @SimpleEvent(description = "User cancelled the Google Play purchase flow.")
  public void PurchaseCancelled() {
    EventDispatcher.dispatchEvent(this, "PurchaseCancelled");
  }

  @SimpleEvent(description = "Current purchased one-time product IDs were refreshed.")
  public void EntitlementsUpdated(YailList productIds) {
    EventDispatcher.dispatchEvent(this, "EntitlementsUpdated", productIds);
  }

  @SimpleEvent(description = "Billing operation failed. operation identifies the failed step.")
  public void BillingError(String operation, int responseCode, String message) {
    EventDispatcher.dispatchEvent(this, "BillingError", operation, responseCode, message);
  }

  @Override
  public void onResume() {
    if (initializeRequested) {
      ensureClient();
      if (IsReady()) {
        queryOwnedPurchases();
      } else {
        connectIfNeeded();
      }
    }
  }

  @Override
  public void onDestroy() {
    connecting = false;
    if (billingClient != null) {
      try {
        billingClient.endConnection();
      } catch (Throwable ignored) {
      }
    }
    billingClient = null;
    productCache.clear();
    ownedProducts.clear();
  }

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
    rememberResult(billingResult);
    int code = billingResult == null ? BillingClient.BillingResponseCode.ERROR : billingResult.getResponseCode();
    if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
      for (Purchase purchase : purchases) processPurchase(purchase, true);
      return;
    }
    if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
      PurchaseCancelled();
      return;
    }
    fail("onPurchasesUpdated", code, debugMessage(billingResult));
  }

  private void ensureClient() {
    if (billingClient != null) return;
    PendingPurchasesParams pending = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build();
    billingClient = BillingClient.newBuilder(activity.getApplicationContext())
        .setListener(this)
        .enablePendingPurchases(pending)
        .enableAutoServiceReconnection()
        .build();
  }

  private void connectIfNeeded() {
    if (billingClient == null || billingClient.isReady() || connecting) return;
    connecting = true;
    billingClient.startConnection(new BillingClientStateListener() {
      @Override public void onBillingSetupFinished(BillingResult billingResult) {
        connecting = false;
        rememberResult(billingResult);
        if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
          lastError = "";
          BillingReady();
          queryOwnedPurchases();
        } else {
          fail("Initialize", billingResult == null ? BillingClient.BillingResponseCode.ERROR : billingResult.getResponseCode(), debugMessage(billingResult));
        }
      }
      @Override public void onBillingServiceDisconnected() {
        connecting = false;
        BillingDisconnected();
      }
    });
  }

  private interface ReadyAction { void run(); }

  private void withReadyClient(final ReadyAction action) {
    ensureClient();
    if (billingClient.isReady()) {
      action.run();
      return;
    }
    if (connecting) {
      fail("Connection", BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, "Billing connection is still starting. Retry after BillingReady.");
      return;
    }
    connecting = true;
    billingClient.startConnection(new BillingClientStateListener() {
      @Override public void onBillingSetupFinished(BillingResult billingResult) {
        connecting = false;
        rememberResult(billingResult);
        if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
          BillingReady();
          action.run();
        } else {
          fail("Connection", billingResult == null ? BillingClient.BillingResponseCode.ERROR : billingResult.getResponseCode(), debugMessage(billingResult));
        }
      }
      @Override public void onBillingServiceDisconnected() {
        connecting = false;
        BillingDisconnected();
      }
    });
  }

  private void queryProductDetails(final List<String> ids, final boolean launchAfterQuery, final String purchaseProductId) {
    List<QueryProductDetailsParams.Product> products = new ArrayList<QueryProductDetailsParams.Product>();
    for (String id : ids) {
      products.add(QueryProductDetailsParams.Product.newBuilder()
          .setProductId(id)
          .setProductType(BillingClient.ProductType.INAPP)
          .build());
    }
    QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
        .setProductList(products)
        .build();
    billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
      @Override public void onProductDetailsResponse(BillingResult billingResult, QueryProductDetailsResult result) {
        rememberResult(billingResult);
        if (billingResult == null || billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
          fail(launchAfterQuery ? "Purchase.QueryProduct" : "QueryProducts",
              billingResult == null ? BillingClient.BillingResponseCode.ERROR : billingResult.getResponseCode(),
              debugMessage(billingResult));
          return;
        }
        List<ProductDetails> details = result == null ? null : result.getProductDetailsList();
        List<Object> rows = new ArrayList<Object>();
        if (details != null) {
          for (ProductDetails pd : details) {
            productCache.put(pd.getProductId(), pd);
            ProductDetails.OneTimePurchaseOfferDetails offer = chooseOffer(pd);
            String formatted = offer == null ? "" : safe(offer.getFormattedPrice());
            String currency = offer == null ? "" : safe(offer.getPriceCurrencyCode());
            long micros = offer == null ? 0L : offer.getPriceAmountMicros();
            String token = offer == null ? "" : safe(offer.getOfferToken());
            rows.add(YailList.makeList(new Object[]{pd.getProductId(), safe(pd.getName()), safe(pd.getDescription()), formatted, currency, Long.valueOf(micros), token}));
          }
        }
        if (!launchAfterQuery) {
          ProductsLoaded(YailList.makeList(rows));
        } else {
          ProductDetails pd = productCache.get(purchaseProductId);
          if (pd == null) {
            fail("Purchase", BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, "Product was not returned by Google Play: " + purchaseProductId);
          } else {
            launchPurchase(pd);
          }
        }
      }
    });
  }

  private void launchPurchase(ProductDetails pd) {
    BillingFlowParams.ProductDetailsParams.Builder item = BillingFlowParams.ProductDetailsParams.newBuilder()
        .setProductDetails(pd);
    ProductDetails.OneTimePurchaseOfferDetails offer = chooseOffer(pd);
    if (offer != null && clean(offer.getOfferToken()).length() > 0) {
      item.setOfferToken(offer.getOfferToken());
    }
    List<BillingFlowParams.ProductDetailsParams> items = new ArrayList<BillingFlowParams.ProductDetailsParams>();
    items.add(item.build());
    BillingFlowParams flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(items).build();
    BillingResult result = billingClient.launchBillingFlow(activity, flow);
    rememberResult(result);
    if (result != null && result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
      PurchaseStarted(pd.getProductId());
    } else {
      fail("Purchase.Launch", result == null ? BillingClient.BillingResponseCode.ERROR : result.getResponseCode(), debugMessage(result));
    }
  }

  private void queryOwnedPurchases() {
    if (billingClient == null || !billingClient.isReady()) return;
    QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.INAPP)
        .build();
    billingClient.queryPurchasesAsync(params, new PurchasesResponseListener() {
      @Override public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
        rememberResult(billingResult);
        if (billingResult == null || billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
          fail("RefreshEntitlements", billingResult == null ? BillingClient.BillingResponseCode.ERROR : billingResult.getResponseCode(), debugMessage(billingResult));
          return;
        }
        ownedProducts.clear();
        if (purchases != null) {
          for (Purchase purchase : purchases) processPurchase(purchase, false);
        }
        emitEntitlements();
      }
    });
  }

  private void processPurchase(final Purchase purchase, final boolean emitPurchaseEvent) {
    if (purchase == null) return;
    final String primaryProduct = firstProduct(purchase);
    if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
      if (emitPurchaseEvent) PurchasePending(primaryProduct, safe(purchase.getPurchaseToken()));
      return;
    }
    if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
    addOwnedProducts(purchase);
    if (purchase.isAcknowledged()) {
      if (emitPurchaseEvent) PurchaseCompleted(primaryProduct, safe(purchase.getPurchaseToken()), true);
      if (!emitPurchaseEvent) emitEntitlements();
      return;
    }
    AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchase.getPurchaseToken())
        .build();
    billingClient.acknowledgePurchase(params, new com.android.billingclient.api.AcknowledgePurchaseResponseListener() {
      @Override public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
        rememberResult(billingResult);
        if (billingResult != null && billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
          if (emitPurchaseEvent) PurchaseCompleted(primaryProduct, safe(purchase.getPurchaseToken()), true);
          emitEntitlements();
        } else {
          fail("Acknowledge", billingResult == null ? BillingClient.BillingResponseCode.ERROR : billingResult.getResponseCode(), debugMessage(billingResult));
        }
      }
    });
  }

  private void addOwnedProducts(Purchase purchase) {
    List<String> ids = purchase.getProducts();
    if (ids == null) return;
    for (String id : ids) if (id != null && id.length() > 0) ownedProducts.add(id);
  }

  private void emitEntitlements() {
    List<Object> ids = new ArrayList<Object>();
    for (String id : ownedProducts) ids.add(id);
    EntitlementsUpdated(YailList.makeList(ids));
  }

  private ProductDetails.OneTimePurchaseOfferDetails chooseOffer(ProductDetails pd) {
    if (pd == null) return null;
    try {
      List<ProductDetails.OneTimePurchaseOfferDetails> offers = pd.getOneTimePurchaseOfferDetailsList();
      if (offers != null && offers.size() > 0) return offers.get(0);
    } catch (Throwable ignored) {
    }
    try {
      return pd.getOneTimePurchaseOfferDetails();
    } catch (Throwable ignored) {
      return null;
    }
  }

  private List<String> parseIds(String csv) {
    List<String> out = new ArrayList<String>();
    if (csv == null) return out;
    String[] parts = csv.split(",");
    for (String p : parts) {
      String id = clean(p);
      if (id.length() > 0 && !out.contains(id)) out.add(id);
    }
    return out;
  }

  private String firstProduct(Purchase purchase) {
    List<String> p = purchase.getProducts();
    return p == null || p.size() == 0 ? "" : safe(p.get(0));
  }

  private void rememberResult(BillingResult result) {
    if (result == null) {
      lastResponseCode = BillingClient.BillingResponseCode.ERROR;
      lastError = "BillingResult was null.";
    } else {
      lastResponseCode = result.getResponseCode();
      lastError = safe(result.getDebugMessage());
    }
  }

  private void fail(String operation, int code, String message) {
    lastResponseCode = code;
    lastError = message == null ? "" : message;
    BillingError(operation, code, lastError);
  }

  private String debugMessage(BillingResult result) {
    return result == null ? "BillingResult was null." : safe(result.getDebugMessage());
  }

  private String clean(String s) { return s == null ? "" : s.trim(); }
  private String safe(String s) { return s == null ? "" : s; }
}
