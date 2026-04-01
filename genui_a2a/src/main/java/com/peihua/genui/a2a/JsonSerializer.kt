package com.peihua.genui.a2a

import com.peihua.genui.a2a.core.APIKeySecurityScheme
import com.peihua.genui.a2a.core.DataPart
import com.peihua.genui.a2a.core.FilePart
import com.peihua.genui.a2a.core.FileType
import com.peihua.genui.a2a.core.FileWithBytes
import com.peihua.genui.a2a.core.FileWithUri
import com.peihua.genui.a2a.core.HttpAuthSecurityScheme
import com.peihua.genui.a2a.core.MutualTlsSecurityScheme
import com.peihua.genui.a2a.core.OAuth2SecurityScheme
import com.peihua.genui.a2a.core.OpenIdConnectSecurityScheme
import com.peihua.genui.a2a.core.Part
import com.peihua.genui.a2a.core.SecurityScheme
import com.peihua.genui.a2a.core.TextPart
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val DefaultJson: Json =
    Json {
        serializersModule = SerializersModule {
            polymorphic(SecurityScheme::class) {   // 声明基础类型
                subclass(APIKeySecurityScheme::class, APIKeySecurityScheme.serializer()) // 声明子类型
                subclass(HttpAuthSecurityScheme::class, HttpAuthSecurityScheme.serializer())
                subclass(OAuth2SecurityScheme::class, OAuth2SecurityScheme.serializer())
                subclass(OpenIdConnectSecurityScheme::class, OpenIdConnectSecurityScheme.serializer())
                subclass(MutualTlsSecurityScheme::class, MutualTlsSecurityScheme.serializer())
            }
            polymorphic(Part::class) {
                subclass(TextPart::class, TextPart.serializer())
                subclass(FilePart::class, FilePart.serializer())
                subclass(DataPart::class, DataPart.serializer())
            }
            polymorphic(FileType::class) {
                subclass(FileWithUri::class, FileWithUri.serializer())
                subclass(FileWithBytes::class, FileWithBytes.serializer())
            }
        }
        encodeDefaults = true
        isLenient = true
        allowSpecialFloatingPointValues = true
        allowStructuredMapKeys = true
        prettyPrint = false
        useArrayPolymorphism = false
    }
