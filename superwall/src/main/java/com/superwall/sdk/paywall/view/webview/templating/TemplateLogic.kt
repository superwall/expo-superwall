import com.superwall.sdk.dependencies.VariablesFactory
import com.superwall.sdk.logger.LogLevel
import com.superwall.sdk.logger.LogScope
import com.superwall.sdk.logger.Logger
import com.superwall.sdk.models.events.EventData
import com.superwall.sdk.models.paywall.Paywall
import com.superwall.sdk.models.product.CrossplatformProduct
import com.superwall.sdk.models.product.PlayStoreProduct
import com.superwall.sdk.models.product.ProductItem
import com.superwall.sdk.paywall.view.webview.templating.models.ExperimentTemplate
import com.superwall.sdk.paywall.view.webview.templating.models.FreeTrialTemplate
import com.superwall.sdk.paywall.view.webview.templating.models.JsonVariables
import com.superwall.sdk.paywall.view.webview.templating.models.ProductTemplate
import kotlinx.serialization.json.Json

object TemplateLogic {
    suspend fun getBase64EncodedTemplates(
        json: Json,
        paywall: Paywall,
        event: EventData?,
        factory: VariablesFactory,
        encodeToBase64: (String) -> String,
    ): String {
        val productsTemplate =
            ProductTemplate(
                eventName = "products",
                products =
                    paywall.playStoreProducts.map {
                        ProductItem(
                            name = it.name,
                            entitlements = it.entitlements.toSet(),
                            type =
                                it.storeProduct.let { crossProduct ->
                                    val product = crossProduct as CrossplatformProduct.StoreProduct.PlayStore
                                    ProductItem.StoreProductType.PlayStore(
                                        PlayStoreProduct(
                                            productIdentifier = product.productIdentifier,
                                            basePlanIdentifier = product.basePlanIdentifier,
                                            offer = product.offer,
                                        ),
                                    )
                                },
                            compositeId = it.compositeId,
                        )
                    },
            )

        val variablesTemplate =
            factory.makeJsonVariables(
                products = paywall.productVariables,
                computedPropertyRequests = paywall.computedPropertyRequests,
                event = event,
            )

        val freeTrialTemplate =
            FreeTrialTemplate(
                eventName = "template_substitutions_prefix",
                prefix = if (paywall.isFreeTrialAvailable) "freeTrial" else null,
            )
        val experimentTemplate =
            ExperimentTemplate(
                "experiment",
                paywall.experiment?.id ?: "",
                paywall?.experiment?.variant?.id ?: "",
                paywall?.experiment?.groupId ?: "",
            )
//
//        val swProductTemplate = swProductTemplate(
//            swProductTemplateVariables = paywall.swProductVariablesTemplate ?: emptyList()
//        )

        val encodedTemplates =
            listOf(
                json.encodeToString(ProductTemplate.serializer(), productsTemplate),
                json.encodeToString(JsonVariables.serializer(), variablesTemplate),
                json.encodeToString(FreeTrialTemplate.serializer(), freeTrialTemplate),
                json.encodeToString(ExperimentTemplate.serializer(), experimentTemplate),
//            json.encodeToString(swProductTemplate)
            )
        val templatesString = "[" + encodedTemplates.joinToString(",") + "]"
        Logger.debug(
            LogLevel.debug,
            LogScope.superwallCore,
            "!!! Template Logic: $templatesString",
        )

        return encodeToBase64(templatesString)
    }

//    private fun swProductTemplate(
//        swProductTemplateVariables: List<ProductVariable>
//    ): Json {
//        val variables = mutableMapOf<String, Any>()
//
//        for (variable in swProductTemplateVariables) {
//            variables[variable.type.name] = Json(variable.attributes)
//        }
//
//        val values = mapOf(
//            "event_name" to "template_product_variables",
//            "variables" to variables
//        )
//
//        return Json(values)
//    }
}
