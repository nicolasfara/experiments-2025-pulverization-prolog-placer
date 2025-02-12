package it.unibo.alchemist.model

import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.utils.AlchemistScafiUtils.getNodeProperty
import it.unibo.prolog.{DeviceDeployment, Placement, PrologPlacerManager, SimulationParameters}
import org.apache.commons.math3.random.RandomGenerator

class PrologDeploymentUpdater[T, P <: Position[P]](
    environment: Environment[T, P],
    timeDistribution: TimeDistribution[T],
    random: RandomGenerator,
    isBaseline: Boolean,
    deploymentStrategy: String,
    maxRenewableEnergyApplication: Double,
    maxRenewableEnergyInfrastructural: Double,
    pueApplication: Double,
    pueInfrastructural: Double,
    availableHwApplication: Int,
    availableHwInfrastructural: Int,
) extends AbstractGlobalReaction[T, P](environment, timeDistribution) {
  private val RE_DEPLOYMENT_TIME = 15
  private lazy val placerManager = new PrologPlacerManager[T, P](
    environment,
    random,
    deploymentStrategy,
    SimulationParameters(
      maxRenewableEnergyApplication,
      maxRenewableEnergyInfrastructural,
      pueApplication,
      pueInfrastructural,
      availableHwApplication,
      availableHwInfrastructural,
    ),
  )
  private var lastDeployment: List[DeviceDeployment] = _
  private var isExecuted = false
  private var elapsedTicks = 0

  override protected def executeBeforeUpdateDistribution(): Unit = {
    elapsedTicks += 1
    if (!isBaseline && !isExecuted) {
      updateDeployment()
      isExecuted = true
    }
    if ((isBaseline && !isExecuted) || (!isBaseline && elapsedTicks == RE_DEPLOYMENT_TIME)) {
      updateDeployment()
      isExecuted = true
      elapsedTicks = 0
    }
    updateFootprint()
  }

  private def updateFootprint(): Unit = {
    placerManager.updateTopology()
    val overallPlacements = lastDeployment.flatMap(_.placements)
    val globalFootprint = placerManager.getFootprint(overallPlacements)
    val node = environment.getNodeByID(0)
    node.setConcentration(new SimpleMolecule("Carbon"), globalFootprint.carbon.asInstanceOf[T])
    node.setConcentration(new SimpleMolecule("Energy"), globalFootprint.energy.asInstanceOf[T])
  }

  private def updateDeployment(): Unit = {
    lastDeployment = placerManager.getNewDeployment
    lastDeployment.foreach { case d @ DeviceDeployment(id, _, _, placements) =>
      val currentNode = environment.getNodeByID(id)
      currentNode.setConcentration(new SimpleMolecule("Deployment"), d.asInstanceOf[T])
      placements.foreach(placeComponentPerDevice(_, currentNode))
    }
  }

  private def placeComponentPerDevice(placement: Placement, nodeDevice: Node[T]): Unit = {
    // TODO: fix hw allocation
    val Placement(component, device, hw) = placement
    val allocator = getNodeProperty(nodeDevice, classOf[AllocatorProperty[T, P]])
    allocator.setComponentsAllocation(Map(component.fqn -> device.id))
  }
}
