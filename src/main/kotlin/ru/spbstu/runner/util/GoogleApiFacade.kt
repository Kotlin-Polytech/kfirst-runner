package ru.spbstu.runner.util

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import java.io.File
import java.io.InputStreamReader

object GoogleApiFacade {

    private val APPLICATION_NAME = "KotlinAsFirstStatistics"

    private val DATA_STORE_DIR = File(System.getProperty("user.home"),
            ".credentials/sheets.googleapis.com-kotlin_as_first_statistics")

    private val DATA_STORE_FACTORY = FileDataStoreFactory(DATA_STORE_DIR)

    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()

    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

    private val SCOPES = listOf(
            SheetsScopes.SPREADSHEETS
    )

    val spreadsheetId by lazy {
        this.javaClass.getResourceAsStream("/spreadsheet_secret.id")
                .use {
                    it.reader().use {
                        it.readText()
                    }
                }
    }

    val credential: Credential by lazy {
        val mySecret = this.javaClass.getResourceAsStream("/client_secret.json")
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(mySecret))

        val flow =
                GoogleAuthorizationCodeFlow
                        .Builder(
                                HTTP_TRANSPORT,
                                JSON_FACTORY,
                                clientSecrets,
                                SCOPES
                        )
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build()

        AuthorizationCodeInstalledApp(flow, LocalServerReceiver())
                .authorize("user")
    }

    val sheets: Sheets by lazy {
        Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
    }

    fun createSheet(sheetName: String) {
        try {
            val sheetId = Math.abs(sheetName.hashCode())

            sheets.spreadsheets()
                    .batchUpdate(
                            spreadsheetId,
                            BatchUpdateSpreadsheetRequest().setRequests(
                                    listOf(
                                            Request().setAddSheet(
                                                    AddSheetRequest().setProperties(
                                                            SheetProperties()
                                                                    .setSheetId(sheetId)
                                                                    .setTitle(sheetName)
                                                                    .setSheetType("GRID")
                                                    )
                                            )
                                    )
                            )

                    )
                    .execute()
        } catch (ex: Exception) {
        }
    }

    fun appendToSheet(sheetName: String, data: List<String>) {
        try {
            sheets.spreadsheets().values()
                    .append(
                            spreadsheetId,
                            "$sheetName!A1",
                            ValueRange().setValues(
                                    listOf(data)
                            )
                    )
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()
        } catch (ex: Exception) {
        }
    }

}
