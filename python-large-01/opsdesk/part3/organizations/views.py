from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from slugify import slugify
from sqlalchemy.orm import Session

from opsdesk.db import get_current_user, get_db
from opsdesk.part3.organizations.service import (OrganizationCreate, OrganizationRead, ProjectCreate, UserRoles,
                                                 add_user, create, get, get_by_name, get_by_slug, project_flows,
                                                 project_service)

DbSession = Annotated[Session, Depends(get_db)]
CurrentUser = Annotated[object, Depends(get_current_user)]

router = APIRouter()


@router.post(
    "",
    response_model=OrganizationRead,
)
def create_organization(
    db_session: DbSession,
    organization_in: OrganizationCreate,
    current_user: CurrentUser,
):
    """Create a new organization."""
    if not organization_in.name:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=[{"msg": "An organization name is required."}],
        )
    organization = get_by_name(db_session=db_session, name=organization_in.name)
    if organization:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=[{"msg": "An organization with this name already exists."}],
        )
    if organization_in.id and get(db_session=db_session, organization_id=organization_in.id):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=[{"msg": "An organization with this id already exists."}],
        )
    slug = slugify(organization_in.name, separator="_")
    if get_by_slug(db_session=db_session, slug=slug):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=[{"msg": "An organization with this slug already exists."}],
        )
    # we create the organization
    organization = create(db_session=db_session, organization_in=organization_in)

    # we add the creator as organization owner
    add_user(
        db_session=db_session, organization=organization, user=current_user, role=UserRoles.owner
    )

    # we create the default project
    project_in = ProjectCreate(
        name="default",
        default=True,
        description="Default Dispatch project.",
        organization=organization,
    )
    project = project_service.create(db_session=db_session, project_in=project_in)

    # we initialize the default project
    project_flows.project_init_flow(
        project_id=project.id, organization_slug=organization.slug, db_session=db_session
    )

    return organization
