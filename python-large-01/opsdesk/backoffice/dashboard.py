"""Ops dashboard: aggregate counts over the automation inventory."""

from collections import OrderedDict

from awx.api.generics import APIView
from awx.api.schema import extend_schema_if_available
from awx.api.versioning import reverse
from awx.main import models
from awx.main.utils import get_user_queryset
from django.db.models import OuterRef, Subquery, Sum
from rest_framework.response import Response


class DashboardView(APIView):
    @extend_schema_if_available(extensions={"x-ai-description": "Get aggregate statistics for Tower"})
    def get(self, request, format=None):
        '''Show Dashboard Details'''
        data = OrderedDict()
        data['related'] = {'jobs_graph': reverse('api:dashboard_jobs_graph_view', request=request)}
        user_inventory = get_user_queryset(request.user, models.Inventory)
        inventory_with_failed_hosts = user_inventory.filter(hosts_with_active_failures__gt=0)
        user_inventory_external = user_inventory.filter(has_inventory_sources=True)
        # if there are *zero* inventories, this aggregate query will be None, fall back to 0
        failed_inventory = user_inventory.aggregate(Sum('inventory_sources_with_failures'))['inventory_sources_with_failures__sum'] or 0
        data['inventories'] = {
            'url': reverse('api:inventory_list', request=request),
            'total': user_inventory.count(),
            'total_with_inventory_source': user_inventory_external.count(),
            'job_failed': inventory_with_failed_hosts.count(),
            'inventory_failed': failed_inventory,
        }
        user_inventory_sources = get_user_queryset(request.user, models.InventorySource)
        ec2_inventory_sources = user_inventory_sources.filter(source='ec2')
        ec2_inventory_failed = ec2_inventory_sources.filter(status='failed')
        data['inventory_sources'] = {}
        data['inventory_sources']['ec2'] = {
            'url': reverse('api:inventory_source_list', request=request) + "?source=ec2",
            'failures_url': reverse('api:inventory_source_list', request=request) + "?source=ec2&status=failed",
            'label': 'Amazon EC2',
            'total': ec2_inventory_sources.count(),
            'failed': ec2_inventory_failed.count(),
        }

        user_groups = get_user_queryset(request.user, models.Group)
        groups_inventory_failed = models.Group.objects.filter(inventory_sources__last_job_failed=True).count()
        data['groups'] = {'url': reverse('api:group_list', request=request), 'total': user_groups.count(), 'inventory_failed': groups_inventory_failed}

        user_hosts = get_user_queryset(request.user, models.Host).exclude(inventory__kind='constructed')
        latest_summary_failed = Subquery(models.JobHostSummary.objects.filter(host_id=OuterRef('pk')).order_by('-id').values('failed')[:1])
        user_hosts_failed = user_hosts.annotate(_latest_failed=latest_summary_failed).filter(_latest_failed=True)

        data['hosts'] = {
            'url': reverse('api:host_list', request=request),
            'total': user_hosts.count(),
            'failed': user_hosts_failed.count(),
        }

        user_projects = get_user_queryset(request.user, models.Project)
        user_projects_failed = user_projects.filter(last_job_failed=True)
        data['projects'] = {
            'url': reverse('api:project_list', request=request),
            'failures_url': reverse('api:project_list', request=request) + "?last_job_failed=True",
            'total': user_projects.count(),
            'failed': user_projects_failed.count(),
        }

        git_projects = user_projects.filter(scm_type='git')
        git_failed_projects = git_projects.filter(last_job_failed=True)
        svn_projects = user_projects.filter(scm_type='svn')
        svn_failed_projects = svn_projects.filter(last_job_failed=True)
        archive_projects = user_projects.filter(scm_type='archive')
        archive_failed_projects = archive_projects.filter(last_job_failed=True)
        data['scm_types'] = {}
        data['scm_types']['git'] = {
            'url': reverse('api:project_list', request=request) + "?scm_type=git",
            'label': 'Git',
            'failures_url': reverse('api:project_list', request=request) + "?scm_type=git&last_job_failed=True",
            'total': git_projects.count(),
            'failed': git_failed_projects.count(),
        }
        data['scm_types']['svn'] = {
            'url': reverse('api:project_list', request=request) + "?scm_type=svn",
            'label': 'Subversion',
            'failures_url': reverse('api:project_list', request=request) + "?scm_type=svn&last_job_failed=True",
            'total': svn_projects.count(),
            'failed': svn_failed_projects.count(),
        }
        data['scm_types']['archive'] = {
            'url': reverse('api:project_list', request=request) + "?scm_type=archive",
            'label': 'Remote Archive',
            'failures_url': reverse('api:project_list', request=request) + "?scm_type=archive&last_job_failed=True",
            'total': archive_projects.count(),
            'failed': archive_failed_projects.count(),
        }

        user_list = get_user_queryset(request.user, models.User)
        team_list = get_user_queryset(request.user, models.Team)
        credential_list = get_user_queryset(request.user, models.Credential)
        job_template_list = get_user_queryset(request.user, models.JobTemplate)
        organization_list = get_user_queryset(request.user, models.Organization)
        data['users'] = {'url': reverse('api:user_list', request=request), 'total': user_list.count()}
        data['organizations'] = {'url': reverse('api:organization_list', request=request), 'total': organization_list.count()}
        data['teams'] = {'url': reverse('api:team_list', request=request), 'total': team_list.count()}
        data['credentials'] = {'url': reverse('api:credential_list', request=request), 'total': credential_list.count()}
        data['job_templates'] = {'url': reverse('api:job_template_list', request=request), 'total': job_template_list.count()}
        return Response(data)
