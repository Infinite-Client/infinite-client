package org.infinite.infinite.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.libs.core.features.Feature
import org.infinite.libs.core.features.property.ListProperty
import org.infinite.utils.toLowerSnakeCase
import java.util.concurrent.CompletableFuture

object InfiniteCommand {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerInternal(dispatcher)
        }
    }

    private fun registerInternal(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val rootNode = Commands.literal("infinite")
            .then(createConfigNode("local", InfiniteClient.localFeatures))
            .then(createConfigNode("global", InfiniteClient.globalFeatures))
            .then(
                Commands.literal("clear").executes {
                    InfiniteClient.localFeatures.reset()
                    InfiniteClient.globalFeatures.reset()
                    it.source.sendSuccess({ Component.literal("§a[Infinite] §fCleared all configs.") }, false)
                    1
                },
            )

        dispatcher.register(rootNode)
    }

    private fun createConfigNode(
        scopeName: String,
        categoriesObj: org.infinite.libs.core.features.FeatureCategories<*, *, *, *>,
    ) = Commands.literal(scopeName)
        .then(
            Commands.literal("clear").executes {
                categoriesObj.reset()
                it.source.sendSuccess({ Component.literal("§a[Infinite] §fCleared $scopeName config.") }, false)
                1
            },
        )
        .apply {
            categoriesObj.categories.values.forEach { category ->
                val catId = category::class.qualifiedName?.split(".")?.let {
                    if (it.size >= 2) it[it.size - 2].toLowerSnakeCase() else "unknown"
                } ?: "unknown"

                val categoryNode = Commands.literal(catId)
                    .then(
                        Commands.literal("clear").executes {
                            category.reset()
                            it.source.sendSuccess({ Component.literal("§a[Infinite] §fCleared category $catId") }, false)
                            1
                        },
                    )

                category.features.values.forEach { feature ->
                    val featId = feature::class.simpleName?.toLowerSnakeCase() ?: "unknown"
                    val featureNode = Commands.literal(featId)

                    // 基本操作
                    featureNode.then(
                        Commands.literal("enable").executes {
                            feature.enable()
                            sendStatus(it, feature)
                            1
                        },
                    )
                    featureNode.then(
                        Commands.literal("disable").executes {
                            feature.disable()
                            sendStatus(it, feature)
                            1
                        },
                    )
                    featureNode.then(
                        Commands.literal("toggle").executes {
                            feature.toggle()
                            sendStatus(it, feature)
                            1
                        },
                    )
                    featureNode.then(
                        Commands.literal("clear").executes {
                            feature.reset()
                            sendStatus(it, feature)
                            1
                        },
                    )

                    // プロパティ操作
                    setupPropertyCommands(featureNode, feature)

                    categoryNode.then(featureNode)
                }
                this.then(categoryNode)
            }
        }

    private fun setupPropertyCommands(
        featureNode: com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>,
        feature: Feature,
    ) {
        // GET
        featureNode.then(
            Commands.literal("get").then(
                Commands.argument("property", StringArgumentType.string())
                    .suggests { _, b -> suggestProperties(feature, b) }
                    .executes { ctx ->
                        val propName = StringArgumentType.getString(ctx, "property")
                        feature.ensureAllPropertiesRegistered()
                        val prop = feature.properties[propName]
                        if (prop != null) {
                            ctx.source.sendSuccess({ Component.literal("§a[Infinite] §f$propName = §b${prop.value}") }, false)
                        } else {
                            ctx.source.sendFailure(Component.literal("§cProperty $propName not found."))
                        }
                        1
                    },
            ),
        )

        // SET (Single Value or List Element)
        featureNode.then(
            Commands.literal("set").then(
                Commands.argument("property", StringArgumentType.string())
                    .suggests { _, b -> suggestProperties(feature, b) }
                    .then(
                        Commands.argument("value", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val propName = StringArgumentType.getString(ctx, "property")
                                val valueStr = StringArgumentType.getString(ctx, "value")

                                // 構文解析: "set {list_prop} {index} {value}" かどうか
                                val parts = valueStr.split(" ", limit = 2)
                                val prop = feature.properties[propName]

                                if (prop is ListProperty<*> && parts.size == 2 && parts[0].toIntOrNull() != null) {
                                    // List Replace: set {property} {index} {value}
                                    val index = parts[0].toInt()
                                    applyListReplace(ctx, prop, index, parts[1])
                                } else {
                                    // Normal Set
                                    feature.tryApply(propName, valueStr)
                                    ctx.source.sendSuccess({ Component.literal("§a[Infinite] §fSet §b$propName §fto §e$valueStr") }, false)
                                }
                                1
                            },
                    ),
            ),
        )

        // ADD (List only)
        featureNode.then(
            Commands.literal("add").then(
                Commands.argument("property", StringArgumentType.string())
                    .suggests { _, b -> suggestProperties(feature, b, listOnly = true) }
                    .then(
                        Commands.argument("value", StringArgumentType.greedyString()).executes { ctx ->
                            val propName = StringArgumentType.getString(ctx, "property")
                            val valueStr = StringArgumentType.getString(ctx, "value")
                            val prop = feature.properties[propName]

                            if (prop is ListProperty<*>) {
                                applyListAdd(ctx, prop, valueStr)
                            } else {
                                ctx.source.sendFailure(Component.literal("§c$propName is not a list property."))
                            }
                            1
                        },
                    ),
            ),
        )
    }

    // --- Helper Methods ---

    private fun suggestProperties(feature: Feature, builder: SuggestionsBuilder, listOnly: Boolean = false): CompletableFuture<Suggestions> {
        feature.ensureAllPropertiesRegistered()
        feature.properties.forEach { (name, prop) ->
            if (!listOnly || prop is ListProperty<*>) {
                builder.suggest(name)
            }
        }
        return builder.buildFuture()
    }

    private fun sendStatus(ctx: CommandContext<CommandSourceStack>, feature: Feature) {
        val state = if (feature.isEnabled()) "§aEnabled" else "§cDisabled"
        ctx.source.sendSuccess({ Component.literal("§a[Infinite] §f${feature.name} is now $state") }, false)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyListAdd(ctx: CommandContext<CommandSourceStack>, prop: ListProperty<T>, valueStr: String) {
        // tryApply 内のロジックが List 全体を上書きする設計なのを考慮し、
        // 個別追加は convertElement を経由して行う
        val method = prop::class.java.getDeclaredMethod("convertElement", Any::class.java)
        method.isAccessible = true
        val element = method.invoke(prop, valueStr) as? T
        if (element != null) {
            prop.add(element)
            ctx.source.sendSuccess({ Component.literal("§a[Infinite] §fAdded element to §b${prop.name}") }, false)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> applyListReplace(ctx: CommandContext<CommandSourceStack>, prop: ListProperty<T>, index: Int, valueStr: String) {
        val method = prop::class.java.getDeclaredMethod("convertElement", Any::class.java)
        method.isAccessible = true
        val element = method.invoke(prop, valueStr) as? T
        if (element != null) {
            prop.replaceAt(index, element)
            ctx.source.sendSuccess({ Component.literal("§a[Infinite] §fReplaced §b${prop.name}§f at index §e$index") }, false)
        }
    }
}
