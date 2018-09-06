package org.jetbrains.research.runner.util

import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest

internal val Dependency.asAether: org.eclipse.aether.graph.Dependency
    get() = org.eclipse.aether.graph.Dependency(
            org.eclipse.aether.artifact.DefaultArtifact(
                    this.groupId,
                    this.artifactId,
                    this.type,
                    this.version
            ),
            this.scope,
            this.isOptional
    )

fun getClasspathFromModel(
        model: Model,
        repoSystem: RepositorySystem,
        session: RepositorySystemSession,
        repos: List<RemoteRepository>
): List<ArtifactResult> {

    // TODO: get Kotlin roots from Maven info

    val collectReq = CollectRequest(
            model.dependencies.map { it.asAether },
            null,
            repos
    )
    val depReq = DependencyRequest(collectReq, null)

    val res = repoSystem.resolveDependencies(session, depReq)

    return res.artifactResults
            .filter { it.isResolved }
}
