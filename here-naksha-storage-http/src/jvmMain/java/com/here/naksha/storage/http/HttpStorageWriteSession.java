package com.here.naksha.storage.http;

import com.here.naksha.storage.http.connector.ConnectorInterfaceWriteExecute;
import naksha.model.ILock;
import naksha.model.IWriteSession;
import naksha.model.MemberProcessorMap;
import naksha.model.NakshaContext;
import naksha.base.NakshaError;
import naksha.base.NakshaException;
import naksha.model.objects.NakshaTx;
import naksha.model.request.ErrorResponse;
import naksha.model.request.Response;
import naksha.model.request.WriteRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpStorageWriteSession extends HttpStorageReadSession implements IWriteSession {

    private static final Logger log = LoggerFactory.getLogger(HttpStorageWriteSession.class);

    private final HttpInterface httpInterface;

    public HttpStorageWriteSession(
            NakshaContext context, HttpStorage storage, RequestSender requestSender, HttpInterface httpInterface) {
        super(context, storage, requestSender, httpInterface);
        this.httpInterface = httpInterface;
    }

    @Override
    public @NotNull Response executeWrite(@NotNull WriteRequest request) {
        try {
            switch (httpInterface) {
                case ffwAdapter:
                    return new ErrorResponse(NakshaError.NOT_IMPLEMENTED, "Writing not supported by underlying storage");
                case dataHubConnector:
                    return new ConnectorInterfaceWriteExecute(getNakshaContext(), request, getRequestSender()).execute();
                default:
                    throw new IllegalStateException("Unsupported HTTP interface: " + httpInterface);
            }
        } catch (NakshaException e) {
            log.info("Unexpected error while executing write", e);
            return new ErrorResponse(e.getError());
        } catch (UnsupportedOperationException e) {
            return new ErrorResponse(NakshaError.NOT_IMPLEMENTED, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("We got exception while executing Write request.", e);
            return new ErrorResponse(NakshaError.EXCEPTION, e.getMessage(), e);
        }
    }

    @Override
    public @NotNull ILock acquireSessionLock(@NotNull String lockId) {
        return null;
    }

    @Override
    public @NotNull ILock acquireTransactionLock(@NotNull String lockId) {
        return null;
    }

    @Override
    public void commit() {
        // do nothing
    }

    @Override
    public void rollback() {
        // do nothing
    }

    @Override
    public @NotNull NakshaTx useTransaction() {
        return null;
    }

    @Override
    public @Nullable NakshaTx getTransaction() {
        return null;
    }

    private final @NotNull MemberProcessorMap processors = new MemberProcessorMap();

    @Override
    public @NotNull MemberProcessorMap getProcessors() {
        return processors;
    }
}
