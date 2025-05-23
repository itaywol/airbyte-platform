import { WebBackendConnectionRead } from "@src/core/api/types/AirbyteClient";
import { getWorkspaceId } from "commands/api/workspace";
import { interceptGetConnectionRequest, waitForGetConnectionRequest } from "commands/interceptors";
import { RouteHandler } from "cypress/types/net-stubbing";

const syncEnabledSwitch = "[data-testid='connection-status-switch']";

interface VisitOptions {
  interceptGetHandler?: RouteHandler;
}

export const visit = (connection: WebBackendConnectionRead, tab = "", { interceptGetHandler }: VisitOptions = {}) => {
  interceptGetConnectionRequest(interceptGetHandler);

  cy.visit(`/workspaces/${getWorkspaceId()}/connections/${connection.connectionId}/${tab}`);

  waitForGetConnectionRequest();
};

export const getSyncEnabledSwitch = () => cy.get(syncEnabledSwitch);
