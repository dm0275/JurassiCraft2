package net.timeless.animationapi.client;

import java.util.Map;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.timeless.unilib.client.model.json.TabulaModelHelper;
import net.timeless.unilib.client.model.tools.MowzieModelRenderer;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.model.ModelDinosaur;
import org.jurassicraft.common.entity.base.EntityDinosaur;

/**
 * @author jabelar
 *         This class is used to hold per-entity animation variables for use with
 *         Jabelar's animation tweening system.
 */
@SideOnly(Side.CLIENT)
public class JabelarAnimationHelper
{
    private final EntityDinosaur theEntity;

    private final Map<AnimID, int[][]> mapOfSequences;

    private final MowzieModelRenderer[][] arrayOfPoses;
    private MowzieModelRenderer[] defaultModelRendererArray;
    private MowzieModelRenderer[] nextPose;

    private float[][] currentRotationArray;
    private float[][] currentPositionArray;
    private float[][] currentOffsetArray;

    private final int numParts;

    private AnimID currentSequence;
    private int numPosesInSequence;
    private int currentPose;
    private int numTicksInTween;
    private int currentTickInTween;

    private final boolean inertialTweens;
    private final float baseInertiaFactor;

    /**
     * @param parEntity         the entity to animate from
     * @param parModel          the model to animate
     * @param parNumParts
     * @param parArrayOfPoses   for each pose(-index) an array of posed Renderers
     * @param parMapOfSequences maps from an {@link AnimID} to the sequence of (pose-index, tween-length)
     * @param parInertialTweens
     * @param parInertiaFactor
     */
    public JabelarAnimationHelper(EntityDinosaur parEntity, ModelDinosaur parModel, int parNumParts,
                                  MowzieModelRenderer[][] parArrayOfPoses, Map<AnimID, int[][]> parMapOfSequences,
                                  boolean parInertialTweens, float parInertiaFactor)
    {
        // transfer static animation info from constructor parameters to instance
        theEntity = parEntity;
        numParts = parNumParts;
        arrayOfPoses = parArrayOfPoses;
        mapOfSequences = parMapOfSequences;
        inertialTweens = parInertialTweens;
        baseInertiaFactor = parInertiaFactor;

        init(parModel);

        JurassiCraft.instance.getLogger().debug("Finished JabelarAnimation constructor");
    }

    public void performJabelarAnimations()
    {
        // Allow interruption of the animation if it is a new animation and not currently dying
        if (theEntity.getAnimID() != currentSequence && currentSequence != AnimID.DYING)
        {
            setNextSequence(theEntity.getAnimID());
        }
        performNextTweenTick();
    }

    private void init(ModelDinosaur parModel)
    {
        defaultModelRendererArray = convertPassedInModelToModelRendererArray(parModel);
        setNextSequence(theEntity.getAnimID());
        JurassiCraft.instance.getLogger().info("Initializing to animation sequence = " + theEntity.getAnimID());
        initPose(); // sets the target pose based on sequence
        initTween();

        // copy passed in model into a model renderer array
        // NOTE: this is the array you will actually animate
        defaultModelRendererArray = convertPassedInModelToModelRendererArray(parModel);

        initCurrentPoseArrays();
    }

    private void initPose()
    {
        numPosesInSequence = mapOfSequences.get(currentSequence).length;

        // initialize first pose
        currentPose = 0;
        nextPose = arrayOfPoses[mapOfSequences.get(currentSequence)[currentPose][0]];
    }

    private void initTween()
    {
        numTicksInTween = mapOfSequences.get(currentSequence)[currentPose][1];
        // filter out illegal values in array
        if (numTicksInTween < 1)
        {
            System.err.println("Array of sequences has sequence with num ticks illegal value (< 1)");
            numTicksInTween = 1;
        }
        currentTickInTween = 0;
    }

    private void initCurrentPoseArrays()
    {
        currentRotationArray = new float[numParts][3];
        currentPositionArray = new float[numParts][3];
        currentOffsetArray = new float[numParts][3];

        for (int partIndex = 0; partIndex < numParts; partIndex++)
        {
            for (int axis = 0; axis < 3; axis++) // X, Y, and Z
            {
                currentRotationArray[partIndex][axis] = 0.0F;
                currentPositionArray[partIndex][axis] = 0.0F;
                currentOffsetArray[partIndex][axis] = 0.0F;
            }
        }

        copyTweenToCurrent();
    }

    private void copyTweenToCurrent()
    {
        for (int i = 0; i < numParts; i++)
        {
            currentRotationArray[i][0] = defaultModelRendererArray[i].rotateAngleX;
            currentRotationArray[i][1] = defaultModelRendererArray[i].rotateAngleY;
            currentRotationArray[i][2] = defaultModelRendererArray[i].rotateAngleZ;
            currentPositionArray[i][0] = defaultModelRendererArray[i].rotationPointX;
            currentPositionArray[i][1] = defaultModelRendererArray[i].rotationPointY;
            currentPositionArray[i][2] = defaultModelRendererArray[i].rotationPointZ;
            currentOffsetArray[i][0] = defaultModelRendererArray[i].offsetX;
            currentOffsetArray[i][1] = defaultModelRendererArray[i].offsetY;
            currentOffsetArray[i][2] = defaultModelRendererArray[i].offsetZ;
        }
    }

    private MowzieModelRenderer[] convertPassedInModelToModelRendererArray(ModelDinosaur parModel)
    {
        String[] partNameArray = parModel.getCubeNamesArray();

        MowzieModelRenderer[] modelRendererArray = new MowzieModelRenderer[numParts];

        for (int i = 0; i < numParts; i++)
        {
            modelRendererArray[i] = parModel.getCube(partNameArray[i]);
        }

        return modelRendererArray;
    }

    private void setNextPose()
    {
        nextPose = arrayOfPoses[mapOfSequences.get(currentSequence)[currentPose][0]];
        numTicksInTween = mapOfSequences.get(currentSequence)[currentPose][1];
        currentTickInTween = 0;

        if (currentSequence != AnimID.IDLE)
            JurassiCraft.instance.getLogger().info("current sequence for entity ID " + theEntity.getEntityId() + " is " + currentSequence
                    + " out of " + mapOfSequences.size() + " and current pose " + currentPose + " out of "
                    + mapOfSequences.get(currentSequence).length + " with " + numTicksInTween + " ticks in tween");
    }

    private void performNextTweenTick()
    {
        // update the passed in model
        tween();
        copyTweenToCurrent();

        if (incrementTweenTick()) // increments tween tick and returns true if finished pose
        {
            handleFinishedPose();
        }
    }

    private void tween()
    {
        float inertiaFactor = calculateInertiaFactor();

        for (int partIndex = 0; partIndex < numParts; partIndex++)
        {
            if (nextPose == null)
            {
                JurassiCraft.instance.getLogger().error("Trying to tween to a null next pose array");
            }
            else if (nextPose[partIndex] == null)
            {
                JurassiCraft.instance.getLogger().error("The part index " + partIndex + " in next pose is null");
            }
            else if (currentRotationArray == null)
            {
                JurassiCraft.instance.getLogger().error("Trying to tween from a null current rotation array");
            }
            else
            {
                nextTweenRotations(partIndex, inertiaFactor);
                nextTweenPositions(partIndex, inertiaFactor);
                nextTweenOffsets(partIndex, inertiaFactor);
            }
        }
    }

    private float calculateInertiaFactor()
    {
        float inertiaFactor = baseInertiaFactor;
        if (inertialTweens)
        {
            if ((currentTickInTween < (numTicksInTween * 0.25F))
                    && (currentTickInTween >= (numTicksInTween - numTicksInTween * 0.25F)))
            {
                inertiaFactor *= 0.5F;
            }
            else
            {
                inertiaFactor *= 1.5F;
            }
        }

        return inertiaFactor;
    }

    /*
     * Tween of the rotateAngles between current angles and target angles.
     * The tween is linear if inertialTweens = false
     */
    private void nextTweenRotations(int parPartIndex, float inertiaFactor)
    {
        defaultModelRendererArray[parPartIndex].rotateAngleX = currentRotationArray[parPartIndex][0] + inertiaFactor
                * (nextPose[parPartIndex].rotateAngleX - currentRotationArray[parPartIndex][0]) / ticksRemaining();
        defaultModelRendererArray[parPartIndex].rotateAngleY = currentRotationArray[parPartIndex][1] + inertiaFactor
                * (nextPose[parPartIndex].rotateAngleY - currentRotationArray[parPartIndex][1]) / ticksRemaining();
        defaultModelRendererArray[parPartIndex].rotateAngleZ = currentRotationArray[parPartIndex][2] + inertiaFactor
                * (nextPose[parPartIndex].rotateAngleZ - currentRotationArray[parPartIndex][2]) / ticksRemaining();
    }

    /*
     * Tween of the rotatePoints between current positions and target positions.
     * The tween is linear if inertialTweens is false.
     */
    private void nextTweenPositions(int parPartIndex, float inertiaFactor)
    {
        defaultModelRendererArray[parPartIndex].rotationPointX = currentPositionArray[parPartIndex][0] + inertiaFactor
                * (nextPose[parPartIndex].rotationPointX - currentPositionArray[parPartIndex][0]) / ticksRemaining();
        defaultModelRendererArray[parPartIndex].rotationPointY = currentPositionArray[parPartIndex][1] + inertiaFactor
                * (nextPose[parPartIndex].rotationPointY - currentPositionArray[parPartIndex][1]) / ticksRemaining();
        defaultModelRendererArray[parPartIndex].rotationPointZ = currentPositionArray[parPartIndex][2] + inertiaFactor
                * (nextPose[parPartIndex].rotationPointZ - currentPositionArray[parPartIndex][2]) / ticksRemaining();
    }

    /*
     * Tween of the offsets between current offsets and target offsets.
     * The tween is linear if inertialTweens is false.
     */
    private void nextTweenOffsets(int parPartIndex, float inertiaFactor)
    {
        defaultModelRendererArray[parPartIndex].offsetX = currentOffsetArray[parPartIndex][0] + inertiaFactor
                * (nextPose[parPartIndex].offsetX - currentOffsetArray[parPartIndex][0]) / ticksRemaining();
        defaultModelRendererArray[parPartIndex].offsetY = currentOffsetArray[parPartIndex][1] + inertiaFactor
                * (nextPose[parPartIndex].offsetY - currentOffsetArray[parPartIndex][1]) / ticksRemaining();
        defaultModelRendererArray[parPartIndex].offsetZ = currentOffsetArray[parPartIndex][2] + inertiaFactor
                * (nextPose[parPartIndex].offsetZ - currentOffsetArray[parPartIndex][2]) / ticksRemaining();
    }

    private int ticksRemaining()
    {
        return (numTicksInTween - currentTickInTween);
    }

    private void handleFinishedPose()
    {
        if (incrementCurrentPose()) // increments pose and returns true if finished sequence
        {
            setNextSequence(theEntity.getAnimID());
        }

        setNextPose();
    }

    // boolean returned indicates if tween was finished
    public boolean incrementTweenTick()
    {
//        JurassiCraft.instance.getLogger().info("current tween step = "+currentTickInTween);
        currentTickInTween++;
        if (currentTickInTween >= numTicksInTween)
            return true;
        return false;
    }

    // boolean returned indicates if sequence was finished
    public boolean incrementCurrentPose()
    {
        // increment current sequence step
        currentPose++;
        // check if finished sequence
        if (currentPose >= numPosesInSequence)
        {
            if (theEntity.getAnimID() == AnimID.DYING) // hold last dying pose indefinitely
            {
                currentPose--;
            }
            else
            {
                currentPose = 0;
                return true;
            }
        }

        // JurassiCraft.instance.getLogger().debug("Next pose is sequence step = "+currentSequenceStep);
        return false;
    }

    private void setNextSequence(AnimID parSequenceIndex)
    {
        // TODO
        // Should control here which animations are interruptible, in which priority
        // I.e. could reject certain changes depending on what current animation is playing

        // handle case where animation sequence isn't available
        if (mapOfSequences.get(parSequenceIndex) == null)
        {
            JurassiCraft.instance.getLogger().error("Requested an anim id " + parSequenceIndex.toString() + " that doesn't have animation sequence in map for entity " + theEntity.getEntityId());
            currentSequence = AnimID.IDLE;
            theEntity.setAnimID(AnimID.IDLE);
        }
        else if (currentSequence != AnimID.IDLE && currentSequence == parSequenceIndex) // finished sequence but no new sequence set
        {
            JurassiCraft.instance.getLogger().debug("Reverting to idle sequence");
            currentSequence = AnimID.IDLE;
            theEntity.setAnimID(AnimID.IDLE);
        }
        else
        {
            JurassiCraft.instance.getLogger().debug("Setting new sequence to "+parSequenceIndex);
            currentSequence = parSequenceIndex;
        }

        currentPose = 0;
        initPose();
        initTween();
        if (currentSequence != AnimID.IDLE)
            JurassiCraft.instance.getLogger().info("current sequence for entity ID " + theEntity.getEntityId() + " is " + currentSequence
                    + " out of " + mapOfSequences.size() + " and current pose " + currentPose + " out of "
                    + mapOfSequences.get(currentSequence).length + " with " + numTicksInTween + " ticks in tween");
    }

    public int getCurrentPose()
    {
        return currentPose;
    }

    public static ModelDinosaur getTabulaModel(String tabulaModel, int geneticVariant)
    {
        // catch the exception so you can call method without further catching
        try
        {
            return new ModelDinosaur(TabulaModelHelper.parseModel(tabulaModel), null); // okay to use null for animator
            // parameter as we get animator
            // from passed-in model
        }
        catch (Exception e)
        {
            JurassiCraft.instance.getLogger().error("Could not load Tabula model = " + tabulaModel);
        }

        return null;
    }

    public ModelDinosaur getTabulaModel(String tabulaModel)
    {
        return getTabulaModel(tabulaModel, 0);
    }
}